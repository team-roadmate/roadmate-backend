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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalkingNetworkService {

    private final NodeRepository nodeRepository;
    private final LinkRepository linkRepository;
    private final ImportLogRepository importLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    @Value("${seoul.api.key}")
    private String apiKey;

    @Value("${seoul.api.base-url}")
    private String baseUrl;

    private static final int PAGE_SIZE = 1000;

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

            // Step 1: Get total count
            String url = String.format("%s/%s/json/TbTraficWlkNet/1/1/%s",
                    baseUrl, apiKey, districtName);
            String response = restTemplate.getForObject(url, String.class);
            ApiResponse apiResponse = gson.fromJson(response, ApiResponse.class);

            if (apiResponse.getTbTraficWlkNet() == null) {
                throw new RuntimeException("Invalid API response");
            }

            int totalCount = apiResponse.getTbTraficWlkNet().getListTotalCount();
            log.info("Total records for {}: {}", districtName, totalCount);

            // Step 2: Process in pages
            for (int start = 1; start <= totalCount; start += PAGE_SIZE) {
                int end = Math.min(start + PAGE_SIZE - 1, totalCount);
                url = String.format("%s/%s/json/TbTraficWlkNet/%d/%d/%s",
                        baseUrl, apiKey, start, end, districtName);

                response = restTemplate.getForObject(url, String.class);
                apiResponse = gson.fromJson(response, ApiResponse.class);

                if (apiResponse.getTbTraficWlkNet().getRow() == null) continue;

                for (ApiResponse.Row row : apiResponse.getTbTraficWlkNet().getRow()) {
                    if ("NODE".equals(row.getNodeType())) {
                        processNode(row);
                        nodeCount++;
                    } else if ("LINK".equals(row.getNodeType())) {
                        int vNodes = processLink(row);
                        virtualNodeCount += vNodes;
                        linkCount++;
                    }
                }

                log.info("Processed {}/{} records", end, totalCount);
            }

            importLog.setTotalNodes(nodeCount);
            importLog.setTotalLinks(linkCount);
            importLog.setVirtualNodes(virtualNodeCount);
            importLog.setStatus("SUCCESS");
            importLog.setCompletedAt(LocalDateTime.now());
            importLogRepository.save(importLog);

            return String.format("Success: Nodes=%d, Links=%d, VirtualNodes=%d",
                    nodeCount, linkCount, virtualNodeCount);

        } catch (Exception e) {
            log.error("Import failed for {}", districtName, e);
            importLog.setStatus("FAILED");
            importLog.setErrorMessage(e.getMessage());
            importLog.setCompletedAt(LocalDateTime.now());
            importLogRepository.save(importLog);
            throw new RuntimeException("Import failed: " + e.getMessage());
        }
    }

    private void processNode(ApiResponse.Row row) {
        if (nodeRepository.existsByNodeId(row.getNodeId())) {
            return;
        }

        double[] coords = parsePoint(row.getNodeWkt());
        Node node = Node.builder()
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

        nodeRepository.save(node);
    }

    private int processLink(ApiResponse.Row row) {
        if (linkRepository.existsByLinkId(row.getLnkgId())) {
            return 0;
        }

        int virtualCount = 0;

        // Check start node
        if (!nodeRepository.existsByNodeId(row.getBgngLnkgId())) {
            double[] coords = parseLineStart(row.getLnkgWkt());
            createVirtualNode(row.getBgngLnkgId(), coords);
            virtualCount++;
        }

        // Check end node
        if (!nodeRepository.existsByNodeId(row.getEndLnkgId())) {
            double[] coords = parseLineEnd(row.getLnkgWkt());
            createVirtualNode(row.getEndLnkgId(), coords);
            virtualCount++;
        }

        Link link = Link.builder()
                .linkId(row.getLnkgId())
                .startNodeId(row.getBgngLnkgId())
                .endNodeId(row.getEndLnkgId())
                .length(row.getLnkgLen())
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

        linkRepository.save(link);
        return virtualCount;
    }

    private void createVirtualNode(String nodeId, double[] coords) {
        Node virtualNode = Node.builder()
                .nodeId(nodeId)
                .latitude(coords[1])
                .longitude(coords[0])
                .isVirtual(true)
                .build();
        nodeRepository.save(virtualNode);
    }

    // Parse POINT(lon lat)
    private double[] parsePoint(String wkt) {
        Pattern pattern = Pattern.compile("POINT\\(([0-9.]+)\\s+([0-9.]+)\\)");
        Matcher matcher = pattern.matcher(wkt);
        if (matcher.find()) {
            return new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2))
            };
        }
        throw new RuntimeException("Invalid POINT format: " + wkt);
    }

    // Parse LINESTRING first coordinate
    private double[] parseLineStart(String wkt) {
        Pattern pattern = Pattern.compile("LINESTRING\\(([0-9.]+)\\s+([0-9.]+)");
        Matcher matcher = pattern.matcher(wkt);
        if (matcher.find()) {
            return new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2))
            };
        }
        throw new RuntimeException("Invalid LINESTRING format: " + wkt);
    }

    // Parse LINESTRING last coordinate
    private double[] parseLineEnd(String wkt) {
        Pattern pattern = Pattern.compile("([0-9.]+)\\s+([0-9.]+)\\)$");
        Matcher matcher = pattern.matcher(wkt);
        if (matcher.find()) {
            return new double[]{
                    Double.parseDouble(matcher.group(1)),
                    Double.parseDouble(matcher.group(2))
            };
        }
        throw new RuntimeException("Invalid LINESTRING format: " + wkt);
    }
}