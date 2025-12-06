package com.trm.roadmate_backend.service;

import com.google.gson.Gson;
import com.trm.roadmate_backend.dto.ApiResponse;
import com.trm.roadmate_backend.entity.ImportLog;
import com.trm.roadmate_backend.entity.Link;
import com.trm.roadmate_backend.entity.Node;
import com.trm.roadmate_backend.repository.ImportLogRepository;
import com.trm.roadmate_backend.repository.LinkRepository;
import com.trm.roadmate_backend.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalkingNetworkService {

    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;
    private final ImportLogRepository importLogRepository;
    private final GraphService graphService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    @Value("${seoul.api.key}")
    private String apiKey;

    @Value("${seoul.api.base-url}")
    private String baseUrl;

    private static final int PAGE_SIZE = 1000;
    private static final int BATCH_SIZE = 2000; // saveAll 배치 크기

    @Transactional
    public String importDistrictData(String districtName) {
        ImportLog importLog = ImportLog.builder()
                .sggNm(districtName)
                .status("RUNNING")
                .startedAt(LocalDateTime.now())
                .build();
        importLogRepository.save(importLog);

        try {
            int nodeCount = 0;
            int linkCount = 0;
            int virtualNodeCount = 0;

            Map<String, Node> nodeBatch = new HashMap<>();
            Map<String, Link> linkBatch = new HashMap<>();

            // 1) Get total count
            String url = String.format("%s/%s/json/TbTraficWlkNet/1/1/%s", baseUrl, apiKey, districtName);
            String response = restTemplate.getForObject(url, String.class);
            ApiResponse apiResponse = gson.fromJson(response, ApiResponse.class);

            if (apiResponse.getTbTraficWlkNet() == null) {
                throw new RuntimeException("Invalid API response");
            }

            int totalCount = apiResponse.getTbTraficWlkNet().getListTotalCount();
            log.info("Total records for {}: {}", districtName, totalCount);

            // 2) Paging
            for (int start = 1; start <= totalCount; start += PAGE_SIZE) {
                int end = Math.min(start + PAGE_SIZE - 1, totalCount);
                url = String.format("%s/%s/json/TbTraficWlkNet/%d/%d/%s", baseUrl, apiKey, start, end, districtName);

                response = restTemplate.getForObject(url, String.class);
                apiResponse = gson.fromJson(response, ApiResponse.class);

                if (apiResponse.getTbTraficWlkNet().getRow() == null) continue;

                // --- NODE 먼저 처리 ---
                for (ApiResponse.Row row : apiResponse.getTbTraficWlkNet().getRow()) {
                    if ("NODE".equals(row.getNodeType())) {
                        if (!nodeRepository.existsByNodeId(row.getNodeId()) && !nodeBatch.containsKey(row.getNodeId())) {
                            Node node = buildNodeFromRow(row);
                            nodeBatch.put(node.getNodeId(), node);
                            nodeCount++;
                        } else {
                            nodeRepository.findByNodeId(row.getNodeId())
                                    .ifPresent(existing -> {
                                        if (existing.getIsVirtual()) {
                                            Node updated = buildNodeFromRow(row);
                                            updated.setId(existing.getId());
                                            nodeRepository.save(updated);
                                        }
                                    });
                        }

                        if (nodeBatch.size() >= BATCH_SIZE) {
                            saveNodeBatch(nodeBatch); // DB에 반영
                        }
                    }
                }

                // --- LINK 처리 전에 NODE 배치 먼저 DB 저장 ---
                if (!nodeBatch.isEmpty()) saveNodeBatch(nodeBatch);

                // --- LINK 처리 ---
                for (ApiResponse.Row row : apiResponse.getTbTraficWlkNet().getRow()) {
                    if ("LINK".equals(row.getNodeType())) {
                        if (!linkRepository.existsByLinkId(row.getLnkgId()) && !linkBatch.containsKey(row.getLnkgId())) {

                            // 시작/끝 노드 존재 여부 확인 + 필요시 가상 노드 생성
                            if (!nodeRepository.existsByNodeId(row.getBgngLnkgId())) {
                                double[] coords = parseLineStart(row.getLnkgWkt());
                                Node virtualNode = Node.builder()
                                        .nodeId(row.getBgngLnkgId())
                                        .latitude(coords[1])
                                        .longitude(coords[0])
                                        .isVirtual(true)
                                        .build();
                                nodeRepository.save(virtualNode);
                                nodeCount++;
                                virtualNodeCount++;
                            }

                            if (!nodeRepository.existsByNodeId(row.getEndLnkgId())) {
                                double[] coords = parseLineEnd(row.getLnkgWkt());
                                Node virtualNode = Node.builder()
                                        .nodeId(row.getEndLnkgId())
                                        .latitude(coords[1])
                                        .longitude(coords[0])
                                        .isVirtual(true)
                                        .build();
                                nodeRepository.save(virtualNode);
                                nodeCount++;
                                virtualNodeCount++;
                            }

                            // LINK 배치 추가
                            Link link = buildLinkFromRow(row);
                            linkBatch.put(link.getLinkId(), link);
                            linkCount++;

                            if (linkBatch.size() >= BATCH_SIZE) {
                                saveLinkBatch(linkBatch);
                            }
                        }
                    }
                }

                log.info("Processed {}/{} records", end, totalCount);
            }

            // --- 마지막 배치 저장 ---
            if (!nodeBatch.isEmpty()) saveNodeBatch(nodeBatch);
            if (!linkBatch.isEmpty()) saveLinkBatch(linkBatch);

            importLog.setTotalNodes(nodeCount);
            importLog.setTotalLinks(linkCount);
            importLog.setVirtualNodes(virtualNodeCount);
            importLog.setStatus("SUCCESS");
            importLog.setCompletedAt(LocalDateTime.now());
            importLogRepository.save(importLog);

            // 그래프 재로딩
            graphService.reloadGraph();

            return String.format("Success: Nodes=%d, Links=%d, VirtualNodes=%d", nodeCount, linkCount, virtualNodeCount);

        } catch (Exception e) {
            log.error("Import failed for {}", districtName, e);
            importLog.setStatus("FAILED");
            importLog.setErrorMessage(e.getMessage());
            importLog.setCompletedAt(LocalDateTime.now());
            importLogRepository.save(importLog);
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    // --- 배치 저장 헬퍼 ---
    private void saveNodeBatch(Map<String, Node> nodeBatch) {
        List<Node> nodesToSave = new ArrayList<>();
        for (Node n : nodeBatch.values()) {
            if (!nodeRepository.existsByNodeId(n.getNodeId())) {
                nodesToSave.add(n);
            }
        }
        if (!nodesToSave.isEmpty()) nodeRepository.saveAll(nodesToSave);
        nodeBatch.clear();
    }

    private void saveLinkBatch(Map<String, Link> linkBatch) {
        List<Link> linksToSave = new ArrayList<>();
        for (Link l : linkBatch.values()) {
            if (!linkRepository.existsByLinkId(l.getLinkId())) {
                linksToSave.add(l);
            }
        }
        if (!linksToSave.isEmpty()) linkRepository.saveAll(linksToSave);
        linkBatch.clear();
    }

    // --- 헬퍼 메서드들 ---

    private Node buildNodeFromRow(ApiResponse.Row row) {
        double[] coords = parsePoint(row.getNodeWkt());
        return Node.builder()
                .nodeId(row.getNodeId())
                .latitude(coords[1])
                .longitude(coords[0])
                .nodeTypeCd(row.getNodeTypeCd())
                .sggCd(row.getSggCd())
                .sggNm(row.getSggNm())
                .emdCd(row.getEmdCd())
                .emdNm(row.getEmdNm())
                .isVirtual(false)
                .build();
    }

    private Link buildLinkFromRow(ApiResponse.Row row) {
        Double len = row.getLnkgLen();
        double length = len == null ? 0.0 : len;

        return Link.builder()
                .linkId(row.getLnkgId())
                .startNodeId(row.getBgngLnkgId())
                .endNodeId(row.getEndLnkgId())
                .length(length)
                .typeCd(row.getLnkgTypeCd())
                .geometry(row.getLnkgWkt())
                .sggCd(row.getSggCd())
                .sggNm(row.getSggNm())
                .emdCd(row.getEmdCd())
                .emdNm(row.getEmdNm())
                .expnCarRd(row.getExpnCarRd())
                .sbwyNtw(row.getSbwyNtw())
                .brg(row.getBrg())
                .tnl(row.getTnl())
                .ovrp(row.getOvrp())
                .crswk(row.getCrswk())
                .park(row.getPark())
                .bldg(row.getBldg())
                .build();
    }

    private void createVirtualNodeIfNotExists(String nodeId, double[] coords) {
        if (nodeRepository.existsByNodeId(nodeId)) return;

        Node virtualNode = Node.builder()
                .nodeId(nodeId)
                .latitude(coords[1])
                .longitude(coords[0])
                .isVirtual(true)
                .build();
        nodeRepository.save(virtualNode);
    }

    // POINT(lon lat) 파서 - 음수, 소수점, 공백 허용
    private double[] parsePoint(String wkt) {
        Pattern pattern = Pattern.compile("POINT\\s*\\(([-+]?[0-9]*\\.?[0-9]+)\\s+([-+]?[0-9]*\\.?[0-9]+)\\)");
        Matcher matcher = pattern.matcher(wkt);
        if (matcher.find()) {
            return new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2))
            };
        }
        throw new RuntimeException("Invalid POINT format: " + wkt);
    }

    // LINESTRING 처음 좌표 파서
    private double[] parseLineStart(String wkt) {
        Pattern pattern = Pattern.compile("LINESTRING\\s*\\((.*)\\)");
        Matcher matcher = pattern.matcher(wkt);
        if (!matcher.find()) throw new RuntimeException("Invalid LINESTRING format: " + wkt);

        String[] coords = matcher.group(1).trim().split(",");
        String[] first = coords[0].trim().split("\\s+");
        return new double[]{
                Double.parseDouble(first[0]),
                Double.parseDouble(first[1])
        };
    }

    // LINESTRING 마지막 좌표 파서
    private double[] parseLineEnd(String wkt) {
        Pattern pattern = Pattern.compile("LINESTRING\\s*\\((.*)\\)");
        Matcher matcher = pattern.matcher(wkt);
        if (!matcher.find()) throw new RuntimeException("Invalid LINESTRING format: " + wkt);

        String[] coords = matcher.group(1).trim().split(",");
        String[] last = coords[coords.length - 1].trim().split("\\s+");
        return new double[]{
                Double.parseDouble(last[0]),
                Double.parseDouble(last[1])
        };
    }
}
