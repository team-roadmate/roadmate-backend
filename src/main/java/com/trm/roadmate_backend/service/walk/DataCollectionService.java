package com.trm.roadmate_backend.service.walk;

import com.trm.roadmate_backend.dto.walk.*;
import com.trm.roadmate_backend.entity.walk.*;
import com.trm.roadmate_backend.repository.walk.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCollectionService {

    private final WalkingNodeRepository nodeRepository;
    private final WalkingLinkRepository linkRepository;
    private final CollectedDistrictRepository districtRepository;
    private final RestTemplate restTemplate;

    @Value("${seoul.api.key}")
    private String apiKey;

    @Value("${seoul.api.base-url}")
    private String baseUrl;

    @Value("${seoul.api.service-name}")
    private String serviceName;

    /**
     * 특정 구역 데이터 수집
     */
    @Transactional
    public CollectionResponse collectDistrict(String districtName) {
        log.info("=== {} 데이터 수집 시작 ===", districtName);

        try {
            // 1. 기존 데이터 삭제 (재수집인 경우)
            if (districtRepository.existsByDistrictName(districtName)) {
                log.info("기존 {} 데이터 삭제 중...", districtName);
                nodeRepository.deleteByDistrict(districtName);
                linkRepository.deleteByDistrict(districtName);
            }

            // 2. API 호출 및 데이터 수집
            int startIndex = 1;
            int pageSize = 1000;
            int totalNodes = 0;
            int totalLinks = 0;

            Set<String> allProcessedNodes = new HashSet<>();  // 전체 수집 과정에서 중복 체크

            while (true) {
                String url = buildApiUrl(startIndex, startIndex + pageSize - 1, districtName);
                log.debug("API 호출: {}", url);

                SeoulApiResponse response = restTemplate.getForObject(url, SeoulApiResponse.class);

                log.info("API 응답 받음: response={}", response != null ? "OK" : "NULL");
                if (response != null) {
                    log.info("TbTraficWlkNet: {}", response.getTbTraficWlkNet() != null ? "OK" : "NULL");
                    if (response.getTbTraficWlkNet() != null) {
                        log.info("RESULT: {}", response.getTbTraficWlkNet().getRESULT());
                        log.info("list_total_count: {}", response.getTbTraficWlkNet().getList_total_count());
                        log.info("row size: {}", response.getTbTraficWlkNet().getRow() != null ? response.getTbTraficWlkNet().getRow().size() : "NULL");
                    }
                }

                if (response == null ||
                        response.getTbTraficWlkNet() == null ||
                        response.getTbTraficWlkNet().getRow() == null ||
                        response.getTbTraficWlkNet().getRow().isEmpty()) {
                    log.info("더 이상 데이터 없음. 수집 완료");
                    break;
                }

                List<SeoulApiResponse.Row> rows = response.getTbTraficWlkNet().getRow();
                log.info("페이지 데이터 수: {}", rows.size());

                // 3. 데이터 파싱 및 저장
                List<WalkingNode> nodesToSave = new ArrayList<>();
                List<WalkingLink> linksToSave = new ArrayList<>();

                for (SeoulApiResponse.Row row : rows) {
                    // 링크 처리
                    if (row.getLNKG_WKT() != null && !row.getLNKG_WKT().isEmpty()) {
                        // 링크의 시작점/끝점에서 노드 생성
                        String startNodeId = row.getBGNG_LNKG_ID();
                        String endNodeId = row.getEND_LNKG_ID();

                        // 링크의 좌표에서 시작/끝 노드 추출
                        List<double[]> coords = parseLineString(row.getLNKG_WKT());

                        if (!coords.isEmpty()) {
                            // 시작 노드 (전체 수집 과정에서 중복 체크)
                            if (!allProcessedNodes.contains(startNodeId)) {
                                nodesToSave.add(createNodeFromCoords(startNodeId, coords.get(0), districtName, row));
                                allProcessedNodes.add(startNodeId);
                                totalNodes++;
                            }

                            // 끝 노드
                            if (!allProcessedNodes.contains(endNodeId)) {
                                nodesToSave.add(createNodeFromCoords(endNodeId, coords.get(coords.size() - 1), districtName, row));
                                allProcessedNodes.add(endNodeId);
                                totalNodes++;
                            }

                            // 링크 생성
                            linksToSave.add(createLink(row, districtName));
                            totalLinks++;
                        }
                    }
                }

                // 배치 저장 (노드 먼저, 링크 나중에)
                if (!nodesToSave.isEmpty()) {
                    nodeRepository.saveAll(nodesToSave);
                    log.info("노드 {} 개 저장 완료", nodesToSave.size());
                }

                if (!linksToSave.isEmpty()) {
                    linkRepository.saveAll(linksToSave);
                    log.info("링크 {} 개 저장 완료", linksToSave.size());
                }

                // 4. 다음 페이지로
                startIndex += pageSize;

                // API 호출 제한 방지 (1초 대기)
                Thread.sleep(1000);
            }

            // 5. 수집 정보 저장
            saveCollectedDistrict(districtName, totalNodes, totalLinks);

            log.info("=== {} 데이터 수집 완료: 노드 {}, 링크 {} ===",
                    districtName, totalNodes, totalLinks);

            return CollectionResponse.builder()
                    .district(districtName)
                    .nodeCount(totalNodes)
                    .linkCount(totalLinks)
                    .message("데이터 수집 완료")
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("데이터 수집 실패: {}", e.getMessage(), e);
            return CollectionResponse.builder()
                    .district(districtName)
                    .message("데이터 수집 실패: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    /**
     * 좌표로부터 노드 생성 (저장 안 함)
     */
    private WalkingNode createNodeFromCoords(String nodeId, double[] coords, String districtName, SeoulApiResponse.Row row) {
        return WalkingNode.builder()
                .nodeId(nodeId)
                .latitude(coords[1])  // lat
                .longitude(coords[0]) // lng
                .nodeType("교차점")
                .district(districtName)
                .neighborhood(row.getEMD_NM())
                .isCrosswalk("1".equals(row.getCRSWK()) || "Y".equalsIgnoreCase(row.getCRSWK()))
                .isOverpass("1".equals(row.getOVRP()) || "Y".equalsIgnoreCase(row.getOVRP()))
                .isBridge("1".equals(row.getBRG()) || "Y".equalsIgnoreCase(row.getBRG()))
                .isTunnel("1".equals(row.getTNL()) || "Y".equalsIgnoreCase(row.getTNL()))
                .isSubway("1".equals(row.getSBWY_NTW()) || "Y".equalsIgnoreCase(row.getSBWY_NTW()))
                .isPark("1".equals(row.getPARK()) || "Y".equalsIgnoreCase(row.getPARK()))
                .isBuilding("1".equals(row.getBLDG()) || "Y".equalsIgnoreCase(row.getBLDG()))
                .build();
    }

    /**
     * 링크 생성 (저장 안 함)
     */
    private WalkingLink createLink(SeoulApiResponse.Row row, String districtName) {
        return WalkingLink.builder()
                .linkId(row.getLNKG_ID())
                .startNodeId(row.getBGNG_LNKG_ID())
                .endNodeId(row.getEND_LNKG_ID())
                .distance(row.getLNKG_LEN())
                .district(districtName)
                .isElevatedRoad("1".equals(row.getEXPN_CAR_RD()))
                .isSubwayNetwork("1".equals(row.getSBWY_NTW()))
                .isBridge("1".equals(row.getBRG()))
                .isTunnel("1".equals(row.getTNL()))
                .isOverpass("1".equals(row.getOVRP()))
                .isCrosswalk("1".equals(row.getCRSWK()))
                .isPark("1".equals(row.getPARK()))
                .isBuilding("1".equals(row.getBLDG()))
                .build();
    }

    /**
     * 좌표로부터 노드 저장
     */
    private void saveNodeFromCoords(String nodeId, double[] coords, String districtName, SeoulApiResponse.Row row) {
        try {
            WalkingNode node = WalkingNode.builder()
                    .nodeId(nodeId)
                    .latitude(coords[1])  // lat
                    .longitude(coords[0]) // lng
                    .nodeType("교차점")
                    .district(districtName)
                    .neighborhood(row.getEMD_NM())
                    .isCrosswalk("1".equals(row.getCRSWK()) || "Y".equalsIgnoreCase(row.getCRSWK()))
                    .isOverpass("1".equals(row.getOVRP()) || "Y".equalsIgnoreCase(row.getOVRP()))
                    .isBridge("1".equals(row.getBRG()) || "Y".equalsIgnoreCase(row.getBRG()))
                    .isTunnel("1".equals(row.getTNL()) || "Y".equalsIgnoreCase(row.getTNL()))
                    .isSubway("1".equals(row.getSBWY_NTW()) || "Y".equalsIgnoreCase(row.getSBWY_NTW()))
                    .isPark("1".equals(row.getPARK()) || "Y".equalsIgnoreCase(row.getPARK()))
                    .isBuilding("1".equals(row.getBLDG()) || "Y".equalsIgnoreCase(row.getBLDG()))
                    .build();

            nodeRepository.save(node);

        } catch (Exception e) {
            log.warn("노드 저장 실패: {}, 에러: {}", nodeId, e.getMessage());
        }
    }

    /**
     * 노드 저장
     */
    private void saveNode(SeoulApiResponse.Row row, String districtName) {
        try {
            double[] coords = parsePoint(row.getNODE_WKT());

            WalkingNode node = WalkingNode.builder()
                    .nodeId(row.getNODE_ID())
                    .latitude(coords[1])
                    .longitude(coords[0])
                    .nodeType(row.getNODE_TYPE())
                    .district(districtName)
                    .neighborhood(row.getEMD_NM())
                    .isCrosswalk("Y".equalsIgnoreCase(row.getCRSWK()))
                    .isOverpass("Y".equalsIgnoreCase(row.getOVRP()))
                    .isBridge("Y".equalsIgnoreCase(row.getBRG()))
                    .isTunnel("Y".equalsIgnoreCase(row.getTNL()))
                    .isSubway("Y".equalsIgnoreCase(row.getSBWY_NTW()))
                    .isPark("Y".equalsIgnoreCase(row.getPARK()))
                    .isBuilding("Y".equalsIgnoreCase(row.getBLDG()))
                    .build();

            nodeRepository.save(node);

        } catch (Exception e) {
            log.warn("노드 저장 실패: {}, 에러: {}", row.getNODE_ID(), e.getMessage());
        }
    }

    /**
     * 링크 저장
     */
    private void saveLink(SeoulApiResponse.Row row, String districtName) {
        try {
            String startNodeId = row.getBGNG_LNKG_ID();
            String endNodeId = row.getEND_LNKG_ID();

            WalkingLink link = WalkingLink.builder()
                    .linkId(row.getLNKG_ID())
                    .startNodeId(startNodeId)
                    .endNodeId(endNodeId)
                    .distance(row.getLNKG_LEN())
                    .district(districtName)
                    .isElevatedRoad("1".equals(row.getEXPN_CAR_RD()))
                    .isSubwayNetwork("1".equals(row.getSBWY_NTW()))
                    .isBridge("1".equals(row.getBRG()))
                    .isTunnel("1".equals(row.getTNL()))
                    .isOverpass("1".equals(row.getOVRP()))
                    .isCrosswalk("1".equals(row.getCRSWK()))
                    .isPark("1".equals(row.getPARK()))
                    .isBuilding("1".equals(row.getBLDG()))
                    .build();

            linkRepository.save(link);

        } catch (Exception e) {
            log.warn("링크 저장 실패: {}, 에러: {}", row.getLNKG_ID(), e.getMessage());
        }
    }

    /**
     * 수집된 구역 정보 저장
     */
    private void saveCollectedDistrict(String districtName, int nodeCount, int linkCount) {
        CollectedDistrict district = districtRepository
                .findByDistrictName(districtName)
                .orElse(new CollectedDistrict());

        district.setDistrictName(districtName);
        district.setNodeCount(nodeCount);
        district.setLinkCount(linkCount);
        district.setUpdatedAt(LocalDateTime.now());

        if (district.getId() == null) {
            district.setCollectedAt(LocalDateTime.now());
        }

        districtRepository.save(district);
    }

    /**
     * WKT LINESTRING 파싱
     */
    private List<double[]> parseLineString(String wkt) {
        // "LINESTRING(126.840 37.489, 126.841 37.490)"
        // → [[126.840, 37.489], [126.841, 37.490]]
        List<double[]> result = new ArrayList<>();
        try {
            String coords = wkt.replace("LINESTRING(", "").replace(")", "").trim();
            String[] points = coords.split(",");

            for (String point : points) {
                String[] parts = point.trim().split("\\s+");
                if (parts.length >= 2) {
                    result.add(new double[]{
                            Double.parseDouble(parts[0]), // lng
                            Double.parseDouble(parts[1])  // lat
                    });
                }
            }
        } catch (Exception e) {
            log.warn("LINESTRING 파싱 실패: {}", wkt);
        }
        return result;
    }

    /**
     * WKT POINT 파싱
     */
    private double[] parsePoint(String wkt) {
        // "POINT(126.856 37.495)" → [126.856, 37.495]
        String coords = wkt.replace("POINT(", "").replace(")", "").trim();
        String[] parts = coords.split("\\s+");
        return new double[]{
                Double.parseDouble(parts[0]), // longitude
                Double.parseDouble(parts[1])  // latitude
        };
    }

    /**
     * API URL 생성
     */
    private String buildApiUrl(int start, int end, String districtName) {
        return String.format("%s/%s/json/%s/%d/%d/%s",
                baseUrl, apiKey, serviceName, start, end, districtName);
    }

    /**
     * 수집된 구역 목록 조회
     */
    public List<DistrictInfo> getCollectedDistricts() {
        return districtRepository.findAll().stream()
                .map(d -> DistrictInfo.builder()
                        .districtName(d.getDistrictName())
                        .nodeCount(d.getNodeCount())
                        .linkCount(d.getLinkCount())
                        .collectedAt(d.getCollectedAt().toString())
                        .build())
                .toList();
    }
}