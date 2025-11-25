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

            while (true) {
                String url = buildApiUrl(startIndex, startIndex + pageSize - 1, districtName);
                log.debug("API 호출: {}", url);

                SeoulApiResponse response = restTemplate.getForObject(url, SeoulApiResponse.class);

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
                for (SeoulApiResponse.Row row : rows) {
                    // 노드 저장
                    if (row.getNODE_WKT() != null && !row.getNODE_WKT().isEmpty()) {
                        saveNode(row, districtName);
                        totalNodes++;
                    }

                    // 링크 저장
                    if (row.getLNKG_WKT() != null && !row.getLNKG_WKT().isEmpty()) {
                        saveLink(row, districtName);
                        totalLinks++;
                    }
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
            WalkingLink link = WalkingLink.builder()
                    .linkId(row.getLNKG_ID())
                    .startNodeId(row.getBGNG_LNKG_ID())
                    .endNodeId(row.getEND_LNKG_ID())
                    .distance(row.getLNKG_LEN())
                    .district(districtName)
                    .isElevatedRoad("Y".equalsIgnoreCase(row.getEXPN_CAR_RD()))
                    .isSubwayNetwork("Y".equalsIgnoreCase(row.getSBWY_NTW()))
                    .isBridge("Y".equalsIgnoreCase(row.getBRG()))
                    .isTunnel("Y".equalsIgnoreCase(row.getTNL()))
                    .isOverpass("Y".equalsIgnoreCase(row.getOVRP()))
                    .isCrosswalk("Y".equalsIgnoreCase(row.getCRSWK()))
                    .isPark("Y".equalsIgnoreCase(row.getPARK()))
                    .isBuilding("Y".equalsIgnoreCase(row.getBLDG()))
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