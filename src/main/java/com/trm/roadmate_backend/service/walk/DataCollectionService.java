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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataCollectionService {

    private final WalkingNodeRepository nodeRepository;
    private final WalkingLinkRepository linkRepository;
    private final DistrictRepository districtRepository;
    private final RestTemplate restTemplate;
    private final GraphService graphService;

    @PersistenceContext
    private final EntityManager em;

    // --- @Value fields (apiKey, baseUrl, serviceName, requestDelayMs) ---
    @Value("${seoul.api.key}")
    private String apiKey;

    @Value("${seoul.api.base-url}")
    private String baseUrl;

    @Value("${seoul.api.service-name}")
    private String serviceName;

    @Value("${seoul.api.request-delay-ms:1000}")
    private long requestDelayMs;

    /**
     * collectDistrict: XML 원본 구조에 맞게 노드와 링크를 분리하여 저장 (최종)
     */
    @Transactional
    public CollectionResponse collectDistrict(String districtName) {
        log.info("=== {} 데이터 수집 시작 (2단계 전략) ===", districtName);

        try {
            // 1) 기존 데이터 삭제
            if (districtRepository.existsByDistrictName(districtName)) {
                log.info("기존 {} 데이터 삭제 중...", districtName);
                linkRepository.deleteByDistrictFast(districtName);
                nodeRepository.deleteByDistrictFast(districtName);
            }

            int startIndex = 1;
            int pageSize = 1000;
            int totalNodes = 0;
            int totalLinks = 0;

            // ===== 1단계: 모든 노드 먼저 수집 =====
            log.info("1단계: 노드 수집 시작");
            Set<String> allNodeIds = new HashSet<>();
            startIndex = 1;

            while (true) {
                String url = buildApiUrl(startIndex, startIndex + pageSize - 1, districtName);
                SeoulApiResponse response = restTemplate.getForObject(url, SeoulApiResponse.class);

                if (response == null ||
                        response.getTbTraficWlkNet() == null ||
                        response.getTbTraficWlkNet().getRow() == null ||
                        response.getTbTraficWlkNet().getRow().isEmpty()) {
                    break;
                }

                List<SeoulApiResponse.Row> rows = response.getTbTraficWlkNet().getRow();
                List<WalkingNode> nodesToSave = new ArrayList<>();
                Set<String> pageNodeIds = new HashSet<>();

                // NODE 타입만 처리
                for (SeoulApiResponse.Row row : rows) {
                    String nodeType = row.getNODE_TYPE();
                    if (nodeType != null && "NODE".equalsIgnoreCase(nodeType.trim())) {
                        String nodeId = normalizeId(row.getNODE_ID());
                        if (row.getNODE_WKT() != null && !row.getNODE_WKT().isEmpty()
                                && pageNodeIds.add(nodeId)) {
                            nodesToSave.add(createNodeFromApiRow(row, districtName));
                        }
                    }
                }

                // DB 중복 체크 및 저장
                if (!nodesToSave.isEmpty()) {
                    Set<String> nodeIdsPage = nodesToSave.stream()
                            .map(WalkingNode::getNodeId)
                            .collect(Collectors.toSet());

                    List<WalkingNode> exist = nodeRepository.findByNodeIdIn(nodeIdsPage);
                    Set<String> existIds = exist.stream()
                            .map(WalkingNode::getNodeId)
                            .collect(Collectors.toSet());

                    List<WalkingNode> filteredNodes = nodesToSave.stream()
                            .filter(n -> !existIds.contains(n.getNodeId()))
                            .collect(Collectors.toList());

                    if (!filteredNodes.isEmpty()) {
                        nodeRepository.saveAll(filteredNodes);
                        em.flush();

                        filteredNodes.forEach(n -> allNodeIds.add(n.getNodeId()));
                        totalNodes += filteredNodes.size();
                        log.info("노드 배치 저장: {} 건 (누적: {})", filteredNodes.size(), totalNodes);
                    }
                }

                em.clear();
                startIndex += pageSize;
                Thread.sleep(requestDelayMs);
            }

            log.info("1단계 완료: 총 {} 개 노드 수집", totalNodes);

            // ===== 2단계: 모든 링크 수집 (이제 모든 노드가 DB에 존재) =====
            log.info("2단계: 링크 수집 시작");
            startIndex = 1;

            // 2단계에서 임시로 생성되어 저장될 노드 목록
            List<WalkingNode> newNodesFromLinks = new ArrayList<>();

            while (true) {
                String url = buildApiUrl(startIndex, startIndex + pageSize - 1, districtName);
                SeoulApiResponse response = restTemplate.getForObject(url, SeoulApiResponse.class);

                if (response == null ||
                        response.getTbTraficWlkNet() == null ||
                        response.getTbTraficWlkNet().getRow() == null ||
                        response.getTbTraficWlkNet().getRow().isEmpty()) {
                    break;
                }

                List<SeoulApiResponse.Row> rows = response.getTbTraficWlkNet().getRow();
                List<WalkingLink> linksToSave = new ArrayList<>();
                Set<String> pageLinkIds = new HashSet<>();

                // --- 수정: 링크를 기반으로 누락된 노드 생성 및 저장 로직 ---
                for (SeoulApiResponse.Row row : rows) {
                    String nodeType = row.getNODE_TYPE();
                    if (nodeType != null && "LINK".equalsIgnoreCase(nodeType.trim())) {
                        String startNodeId = normalizeId(row.getBGNG_LNKG_ID());
                        String endNodeId = normalizeId(row.getEND_LNKG_ID());
                        String linkId = normalizeId(row.getLNKG_ID());
                        String linkWkt = row.getLNKG_WKT();

                        if (linkId == null || linkWkt == null) continue;

                        double[] coords = parseLineString(linkWkt);
                        if (coords.length != 4) continue; // 좌표 파싱 실패

                        boolean startNodeMissing = !allNodeIds.contains(startNodeId);
                        boolean endNodeMissing = !allNodeIds.contains(endNodeId);

                        // 3. 누락된 노드 생성
                        if (startNodeMissing) {
                            // 시작 노드 생성
                            if (allNodeIds.add(startNodeId)) { // Set에 추가 성공했다면 DB에도 없는 노드
                                WalkingNode newNode = createBoundaryNode(startNodeId, districtName, coords[0], coords[1]);
                                newNodesFromLinks.add(newNode);
                                log.warn("링크 기반 시작 노드 생성 및 추가: {}", startNodeId);
                            }
                        }
                        if (endNodeMissing) {
                            // 종료 노드 생성
                            if (allNodeIds.add(endNodeId)) { // Set에 추가 성공했다면 DB에도 없는 노드
                                WalkingNode newNode = createBoundaryNode(endNodeId, districtName, coords[2], coords[3]);
                                newNodesFromLinks.add(newNode);
                                log.warn("링크 기반 종료 노드 생성 및 추가: {}", endNodeId);
                            }
                        }
                    }
                }

                // 4. 생성된 노드 배치 저장 (필수!)
                if (!newNodesFromLinks.isEmpty()) {
                    nodeRepository.saveAll(newNodesFromLinks);
                    em.flush();
                    totalNodes += newNodesFromLinks.size();
                    log.info("링크 기반 노드 배치 저장: {} 건 (누적 노드: {})", newNodesFromLinks.size(), totalNodes);
                    newNodesFromLinks.clear(); // 목록 초기화
                }
                em.clear();
                // --- 수정 끝 ---

                // --- 기존 링크 저장 로직 (이제 allNodeIds는 새로 생성된 노드를 포함) ---
                for (SeoulApiResponse.Row row : rows) {
                    String nodeType = row.getNODE_TYPE();
                    if (nodeType != null && "LINK".equalsIgnoreCase(nodeType.trim())) {
                        String startNodeId = normalizeId(row.getBGNG_LNKG_ID());
                        String endNodeId = normalizeId(row.getEND_LNKG_ID());
                        String linkId = normalizeId(row.getLNKG_ID());

                        // 양쪽 노드가 존재하는지 확인 (새로 생성된 노드까지 모두 포함됨)
                        if (linkId != null && pageLinkIds.add(linkId)
                                && allNodeIds.contains(startNodeId)
                                && allNodeIds.contains(endNodeId)) {
                            linksToSave.add(createLinkFromApiRow(row, districtName,
                                    startNodeId, endNodeId, linkId));
                        } else if (linkId != null) {
                            // **이 경고는 노드 생성 후에도 NodeId나 LinkId가 null이거나 중복된 경우만 발생**
                            log.warn("링크 스킵 (NodeId/LinkId 문제): {} -> {}", startNodeId, endNodeId);
                        }
                    }
                }

                // DB 중복 체크 및 저장
                if (!linksToSave.isEmpty()) {
                    Set<String> linkIdsPage = linksToSave.stream()
                            .map(WalkingLink::getLinkId)
                            .collect(Collectors.toSet());

                    List<WalkingLink> existLinks = linkRepository.findByLinkIdIn(linkIdsPage);
                    Set<String> existLinkIds = existLinks.stream()
                            .map(WalkingLink::getLinkId)
                            .collect(Collectors.toSet());

                    List<WalkingLink> filteredLinks = linksToSave.stream()
                            .filter(l -> !existLinkIds.contains(l.getLinkId()))
                            .collect(Collectors.toList());

                    if (!filteredLinks.isEmpty()) {
                        linkRepository.saveAll(filteredLinks);
                        em.flush();
                        totalLinks += filteredLinks.size();
                        log.info("링크 배치 저장: {} 건 (누적: {})", filteredLinks.size(), totalLinks);
                    }
                }

                em.clear();
                startIndex += pageSize;
                Thread.sleep(requestDelayMs);
            }

            log.info("2단계 완료: 총 {} 개 링크 수집", totalLinks);

            // 최종 결과 저장
            saveCollectedDistrict(districtName, totalNodes, totalLinks);

            // ✅ 그래프 재구축 (중요!)
            log.info("그래프 재구축 시작...");
            graphService.rebuildGraph();
            log.info("그래프 재구축 완료");

            return CollectionResponse.builder()
                    .district(districtName)
                    .nodeCount(totalNodes)
                    .linkCount(totalLinks)
                    .message("데이터 수집 및 그래프 재구축 완료")
                    .success(true)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("데이터 수집 중 스레드 인터럽트", e);
            throw new RuntimeException("수집 중단", e);
        } catch (Exception e) {
            log.error("데이터 수집 실패", e);
            throw new RuntimeException("데이터 수집 실패", e);
        }
    }

    // normalizeId, createNodeFromApiRow, createLinkFromApiRow 등은 동일
    private String normalizeId(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.matches("^\\d+\\.0+$")) {
            s = s.substring(0, s.indexOf('.'));
        }
        if (s.contains(".")) {
            try {
                double d = Double.parseDouble(s);
                long asLong = (long) d;
                if (Double.compare(d, (double) asLong) == 0) {
                    s = String.valueOf(asLong);
                }
            } catch (NumberFormatException ignored) {}
        }
        return s;
    }

    private WalkingNode createNodeFromApiRow(SeoulApiResponse.Row row, String district) {
        double[] coords = parsePoint(row.getNODE_WKT());
        if (coords.length == 0) {
            coords = new double[]{0.0, 0.0};
        }

        return WalkingNode.builder()
                .nodeId(normalizeId(row.getNODE_ID()))
                .longitude(coords[0])
                .latitude(coords[1])
                .district(district)
                .nodeType(row.getNODE_TYPE())
                .neighborhood(row.getEMD_NM())
                .isCrosswalk("1".equals(row.getCRSWK()))
                .isOverpass("1".equals(row.getOVRP()))
                .isBridge("1".equals(row.getBRG()))
                .isTunnel("1".equals(row.getTNL()))
                .isSubway("1".equals(row.getSBWY_NTW()))
                .isPark("1".equals(row.getPARK()))
                .isBuilding("1".equals(row.getBLDG()))
                .build();
    }

    /**
     * 링크의 좌표를 기반으로 누락된 노드를 생성 (최소 정보만 부여)
     */
    private WalkingNode createBoundaryNode(String nodeId, String district, double lon, double lat) {
        return WalkingNode.builder()
                .nodeId(nodeId)
                .longitude(lon)
                .latitude(lat)
                .district(district) // 현재 구역으로 임시 지정
                .nodeType("MANUALLY_GENERATED") // 수동 생성 노드임을 표시
                .neighborhood("UNKNOWN")
                .isCrosswalk(false)
                .isOverpass(false)
                .isBridge(false)
                .isTunnel(false)
                .isSubway(false)
                .isPark(false)
                .isBuilding(false)
                .build();
    }

    private WalkingLink createLinkFromApiRow(SeoulApiResponse.Row row, String district,
                                             String startNodeId, String endNodeId, String linkId) {
        return WalkingLink.builder()
                .linkId(linkId)
                .startNodeId(startNodeId)
                .endNodeId(endNodeId)
                .distance(row.getLNKG_LEN())
                .district(district)
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

    private double[] parsePoint(String wkt) {
        try {
            String coords = wkt.replace("POINT(", "").replace(")", "").trim();
            String[] parts = coords.split("\\s+");
            return new double[]{
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1])
            };
        } catch (Exception e) {
            log.warn("POINT 파싱 실패: {}", wkt);
            return new double[0];
        }
    }

    /**
     * LINESTRING WKT를 파싱하여 시작 노드의 좌표와 끝 노드의 좌표를 반환
     * 반환 값: [startLon, startLat, endLon, endLat]
     */
    private double[] parseLineString(String wkt) {
        try {
            String coords = wkt.replace("LINESTRING(", "").replace(")", "").trim();
            String[] parts = coords.split(",\\s*");

            if (parts.length < 2) {
                return new double[0];
            }

            // 시작점 (첫 번째 좌표)
            String[] start = parts[0].trim().split("\\s+");
            // 끝점 (마지막 좌표)
            String[] end = parts[parts.length - 1].trim().split("\\s+");

            if (start.length != 2 || end.length != 2) {
                return new double[0];
            }

            return new double[]{
                    Double.parseDouble(start[0]), // startLon
                    Double.parseDouble(start[1]), // startLat
                    Double.parseDouble(end[0]),   // endLon
                    Double.parseDouble(end[1])    // endLat
            };
        } catch (Exception e) {
            log.error("LINESTRING 파싱 중 오류 발생: {}", wkt, e);
            return new double[0];
        }
    }

    private String buildApiUrl(int start, int end, String districtName) {
        return String.format("%s/%s/json/%s/%d/%d/%s",
                baseUrl, apiKey, serviceName, start, end, districtName);
    }

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
