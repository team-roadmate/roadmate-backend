package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.SeoulApiResponse;
import com.trm.roadmate_backend.entity.*;
import com.trm.roadmate_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional; // Optional 임포트 추가
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalkingNetworkService {

    private final WalkingNodeRepository nodeRepository;
    private final WalkingLinkRepository linkRepository;
    private final ApiCallHistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    @Value("${seoul.api.key}")
    private String apiKey;

    @Value("${seoul.api.base-url}")
    private String baseUrl;

    @Value("${seoul.api.service-name}")
    private String serviceName;

    @Value("${seoul.api.page-size}")
    private int pageSize;

    // 내부 Record 정의 (API 호출 결과를 반환하기 위함)
    private record ApiCallResult(int successCount, int failCount) {}


    /**
     * 주 메서드: 도보 네트워크 데이터 수집 및 저장
     * 노드 데이터를 먼저 저장하여 외래 키 무결성을 보장한 후, 링크 데이터를 저장합니다.
     */
    public ApiCallHistory fetchAndSaveWalkingNetwork(String sggNm) {
        log.info("========== 도보 네트워크 데이터 수집 시작: {} ==========", sggNm);

        ApiCallHistory history = ApiCallHistory.builder()
                .sggNm(sggNm)
                .startTime(LocalDateTime.now())
                .successCount(0)
                .failCount(0)
                .build();

        // 전체 세션 동안 중복을 체크하기 위한 Set
        Set<String> existingNodeIds = new HashSet<>();
        Set<String> existingLinkIds = new HashSet<>();
        int totalSuccessCount = 0;
        int totalFailCount = 0;

        try {
            // 1. 모든 노드 데이터 수집 및 저장 (FK 참조 대상 확보 및 임시 노드 갱신)
            log.info("▶️ 1단계: 노드(NODE) 데이터 수집 및 저장 시작.");
            ApiCallResult nodeResult = processNetworkData(sggNm, "NODE", existingNodeIds, existingLinkIds);
            totalSuccessCount += nodeResult.successCount();
            totalFailCount += nodeResult.failCount();
            log.info("✅ 1단계 완료: 성공 {} 건, 실패 {} 건", nodeResult.successCount(), nodeResult.failCount());

            // 2. 모든 링크 데이터 수집 및 저장 (노드 저장 완료 후 진행, 누락 시 좌표 기반 임시 노드 생성)
            log.info("▶️ 2단계: 링크(LINK) 데이터 수집 및 저장 시작.");
            ApiCallResult linkResult = processNetworkData(sggNm, "LINK", existingNodeIds, existingLinkIds);
            totalSuccessCount += linkResult.successCount();
            totalFailCount += linkResult.failCount();
            log.info("✅ 2단계 완료: 성공 {} 건, 실패 {} 건", linkResult.successCount(), linkResult.failCount());

            // 최종 통계 업데이트
            history.setSuccessCount(totalSuccessCount);
            history.setFailCount(totalFailCount);
            history.setStatus(totalFailCount == 0 ? "SUCCESS" : "PARTIAL");
            history.setEndTime(LocalDateTime.now());
            history.setTotalCount(totalSuccessCount + totalFailCount);

            log.info("========== 수집 완료: 최종 성공 {}, 최종 실패 {} ==========", totalSuccessCount, totalFailCount);

        } catch (Exception e) {
            log.error("데이터 수집 중 오류 발생", e);
            history.setStatus("FAILED");
            history.setErrorMessage(e.getMessage());
            history.setEndTime(LocalDateTime.now());
        }

        return saveHistory(history);
    }

    // --- 보조 메서드 시작 ---

    private ApiCallResult processNetworkData(String sggNm, String targetType,
                                             Set<String> existingNodeIds, Set<String> existingLinkIds) {

        int startIndex = 1;
        int totalCount = 0;
        int successCount = 0;
        int failCount = 0;
        boolean hasMoreData = true;

        while (hasMoreData) {
            int endIndex = startIndex + pageSize - 1;
            String url = buildApiUrl(startIndex, endIndex, sggNm);

            log.info("API 호출: {} ~ {} (자치구: {}, 타입: {})", startIndex, endIndex, sggNm, targetType);

            try {
                SeoulApiResponse response = restTemplate.getForObject(url, SeoulApiResponse.class);

                if (response == null || response.getTbTraficWlkNet() == null) {
                    log.warn("응답 데이터가 없습니다.");
                    break;
                }

                SeoulApiResponse.Result result = response.getTbTraficWlkNet().getResult();
                if (!"INFO-000".equals(result.getCode())) {
                    log.error("API 오류: {} - {}", result.getCode(), result.getMessage());
                    failCount += pageSize;
                    break;
                }

                if (totalCount == 0) {
                    totalCount = response.getTbTraficWlkNet().getListTotalCount();
                }

                List<SeoulApiResponse.NetworkRow> rows = response.getTbTraficWlkNet().getRow();
                if (rows == null || rows.isEmpty()) {
                    hasMoreData = false;
                    break;
                }

                // 필터링 및 배치 저장
                int batchResult = saveBatchByType(rows, targetType, sggNm, existingNodeIds, existingLinkIds);
                successCount += batchResult;

                log.info("저장 완료 ({}): {} 건 (누적: {})", targetType, batchResult, successCount);

                startIndex = endIndex + 1;

                if (startIndex > totalCount) {
                    hasMoreData = false;
                }

                Thread.sleep(200);

            } catch (Exception e) {
                log.error("페이지 처리 중 오류: {} ~ {}", startIndex, endIndex, e);
                failCount += pageSize;
            }
        }
        return new ApiCallResult(successCount, failCount);
    }


    /**
     * 특정 타입에 해당하는 데이터만 필터링하여 저장합니다.
     * 노드 누락 시 좌표 기반 임시 노드를 생성하거나 갱신하는 로직이 포함되어 있습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected int saveBatchByType(List<SeoulApiResponse.NetworkRow> rows, String targetType, String sggNm,
                                  Set<String> existingNodeIds, Set<String> existingLinkIds) {

        int savedCount = 0;

        for (SeoulApiResponse.NetworkRow row : rows) {
            if (targetType.equals(row.getNodeType())) {

                try {
                    if ("NODE".equals(targetType)) {
                        String nodeId = row.getNodeId();
                        if (nodeId != null && !nodeId.isEmpty() && !"0".equals(nodeId)) {

                            // 🔴 Optional 처리 추가: findByNodeId의 반환 타입이 Optional이라고 가정
                            WalkingNode existingNode = nodeRepository.findByNodeId(nodeId)
                                    .orElse(null);

                            if (existingNode == null) {
                                // 1. DB에 없는 경우: 신규 노드로 저장
                                if (!existingNodeIds.contains(nodeId)) {
                                    WalkingNode newNode = buildNodeEntity(row);
                                    if (newNode != null) {
                                        nodeRepository.save(newNode);
                                        existingNodeIds.add(nodeId);
                                        savedCount++;
                                    }
                                }
                            } else if ("TEMP".equals(existingNode.getSggNm())) {
                                // 2. 임시 노드인 경우: 실제 데이터로 갱신 (UPDATE)
                                existingNode.setSggNm(row.getSggNm());
                                existingNode.setNodeCode(row.getNodeTypeCd());

                                if (row.getNodeWkt() != null) {
                                    existingNode.setNodeWkt(row.getNodeWkt());
                                    BigDecimal[] coords = extractCoordinates(row.getNodeWkt());
                                    if (coords != null) {
                                        existingNode.setLongitude(coords[0]);
                                        existingNode.setLatitude(coords[1]);
                                    }
                                }
                                nodeRepository.save(existingNode);
                                log.info("✅ 임시 노드 갱신 완료: {}", nodeId);
                                savedCount++;
                            }
                        }

                    } else if ("LINK".equals(targetType)) {
                        String linkId = row.getLnkgId();
                        String startNodeId = row.getBgngLnkgId();
                        String endNodeId = row.getEndLnkgId();
                        String linkWkt = row.getLnkgWkt();

                        if (linkId != null && !linkId.isEmpty() && !"0".equals(linkId)) {
                            // Link 중복 체크
                            if (!existingLinkIds.contains(linkId) && !linkRepository.existsByLinkId(linkId)) {

                                // ⚠️ 좌표 기반 임시 노드 생성 로직 ⚠️
                                if (linkWkt != null) {

                                    // 1. 시작 노드 확인 및 임시 노드 생성 (링크의 시작 좌표 사용)
                                    if (!nodeRepository.existsByNodeId(startNodeId)) {
                                        BigDecimal[] startCoords = extractCoordinateFromLinkWkt(linkWkt, true);
                                        if (startCoords != null) {
                                            nodeRepository.save(createTemporaryNodeWithCoords(startNodeId, startCoords[0], startCoords[1], linkWkt));
                                            log.warn("▶️ 임시 노드 생성 (시작): {}", startNodeId);
                                        }
                                    }

                                    // 2. 끝 노드 확인 및 임시 노드 생성 (링크의 종료 좌표 사용)
                                    if (!nodeRepository.existsByNodeId(endNodeId)) {
                                        BigDecimal[] endCoords = extractCoordinateFromLinkWkt(linkWkt, false);
                                        if (endCoords != null) {
                                            nodeRepository.save(createTemporaryNodeWithCoords(endNodeId, endCoords[0], endCoords[1], linkWkt));
                                            log.warn("▶️ 임시 노드 생성 (종료): {}", endNodeId);
                                        }
                                    }
                                }

                                // 외래 키가 충족되었으므로 링크 저장
                                WalkingLink link = buildLinkEntity(row);
                                if (link != null) {
                                    linkRepository.save(link);
                                    existingLinkIds.add(linkId);
                                    savedCount++;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("데이터 처리 중 오류: type={}, nodeId={}, linkId={}",
                            row.getNodeType(), row.getNodeId(), row.getLnkgId(), e);
                }
            }
        }
        return savedCount;
    }


    @Transactional
    protected ApiCallHistory saveHistory(ApiCallHistory history) {
        return historyRepository.save(history);
    }

    /**
     * 좌표를 포함하여 임시 노드를 생성합니다.
     */
    private WalkingNode createTemporaryNodeWithCoords(String nodeId, BigDecimal longitude, BigDecimal latitude, String wkt) {
        return WalkingNode.builder()
                .nodeId(nodeId)
                // 임시 노드임을 명시
                .sggNm("TEMP")
                .nodeCode("TEMP")
                .longitude(longitude)
                .latitude(latitude)
                .nodeWkt("POINT(" + longitude.toPlainString() + " " + latitude.toPlainString() + ")") // WKT도 POINT 형태로 변환하여 저장
                .build();
    }

    /**
     * LINESTRING WKT에서 시작점 또는 끝점의 좌표를 추출합니다.
     */
    private BigDecimal[] extractCoordinateFromLinkWkt(String wkt, boolean isStartNode) {
        try {
            // LINESTRING(x1 y1, x2 y2, ..., xn yn) 형태에서 좌표들만 추출
            Pattern coordsPattern = Pattern.compile("LINESTRING\\s*\\((.*)\\)");
            Matcher coordsMatcher = coordsPattern.matcher(wkt);

            if (coordsMatcher.find()) {
                String allCoords = coordsMatcher.group(1);
                String[] points = allCoords.split(",\\s*"); // "x y" 형태의 각 좌표 쌍 분리

                String targetPointStr = isStartNode ? points[0] : points[points.length - 1];
                String[] coords = targetPointStr.split("\\s+"); // "x"와 "y" 분리

                if (coords.length == 2) {
                    BigDecimal longitude = new BigDecimal(coords[0].trim());
                    BigDecimal latitude = new BigDecimal(coords[1].trim());
                    return new BigDecimal[]{longitude, latitude};
                }
            }
        } catch (Exception e) {
            log.warn("링크 WKT에서 좌표 추출 실패: {}", wkt, e);
        }
        return null;
    }


    private WalkingNode buildNodeEntity(SeoulApiResponse.NetworkRow row) {
        // 기존 buildNodeEntity 로직은 변경 없음
        try {
            WalkingNode node = WalkingNode.builder()
                    .nodeId(row.getNodeId())
                    .nodeCode(row.getNodeTypeCd() != null ? row.getNodeTypeCd() : "0")
                    .sggNm(row.getSggNm())
                    .nodeWkt(row.getNodeWkt())
                    .build();

            if (row.getNodeWkt() != null) {
                BigDecimal[] coords = extractCoordinates(row.getNodeWkt());
                if (coords != null) {
                    node.setLongitude(coords[0]);
                    node.setLatitude(coords[1]);
                }
            }

            return node;
        } catch (Exception e) {
            log.error("노드 엔티티 생성 실패: {}", row.getNodeId(), e);
            return null;
        }
    }

    private WalkingLink buildLinkEntity(SeoulApiResponse.NetworkRow row) {
        // 기존 buildLinkEntity 로직은 변경 없음
        try {
            WalkingLink link = WalkingLink.builder()
                    .linkId(row.getLnkgId())
                    .linkCode(row.getLnkgTypeCd() != null ? row.getLnkgTypeCd() : "0000")
                    .startNodeId(row.getBgngLnkgId())
                    .endNodeId(row.getEndLnkgId())
                    .sggNm(row.getSggNm())
                    .linkWkt(row.getLnkgWkt())
                    .build();

            if (row.getLnkgLen() != null) {
                try {
                    link.setLinkLength(new BigDecimal(row.getLnkgLen()));
                } catch (Exception e) {
                    log.warn("길이 파싱 실패: {}", row.getLnkgLen());
                }
            }

            return link;
        } catch (Exception e) {
            log.error("링크 엔티티 생성 실패: {}", row.getLnkgId(), e);
            return null;
        }
    }

    // 기존의 POINT WKT 추출 메서드 (노드 데이터용)
    private BigDecimal[] extractCoordinates(String wkt) {
        try {
            Pattern pattern = Pattern.compile("POINT\\s*\\(\\s*([0-9.]+)\\s+([0-9.]+)\\s*\\)");
            Matcher matcher = pattern.matcher(wkt);

            if (matcher.find()) {
                BigDecimal longitude = new BigDecimal(matcher.group(1));
                BigDecimal latitude = new BigDecimal(matcher.group(2));
                return new BigDecimal[]{longitude, latitude};
            }
        } catch (Exception e) {
            log.warn("좌표 추출 실패: {}", wkt);
        }
        return null;
    }

    private String buildApiUrl(int start, int end, String sggNm) {
        return String.format("%s/%s/json/%s/%d/%d/%s",
                baseUrl, apiKey, serviceName, start, end, sggNm);
    }

    public long getNodeCount(String sggNm) {
        return nodeRepository.countBySggNm(sggNm);
    }

    public long getLinkCount(String sggNm) {
        return linkRepository.countBySggNm(sggNm);
    }
}