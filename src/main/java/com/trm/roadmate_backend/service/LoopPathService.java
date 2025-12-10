package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.*;
import com.trm.roadmate_backend.entity.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoopPathService {

    private final GraphService graphService;
    private final PathfindingService pathfindingService;

    private static final double R = 6371000; // 지구 반지름 (미터)

    // ==================== 내부 클래스 ====================

    private static class DeviationPoints {
        double latA, lonA, latB, lonB;
    }

    // ==================== 1. Estimate: 루프 가능 여부 및 거리 범위 계산 ====================

    public LoopEstimateResponse estimateLoop(LoopEstimateRequest request) {
        double straightDist = calculateHaversine(
                request.getStartLat(), request.getStartLng(),
                request.getViaLat(), request.getViaLng()
        ) / 1000.0;

        String startNodeId = findNearestNodeId(request.getStartLat(), request.getStartLng());
        String viaNodeId = findNearestNodeId(request.getViaLat(), request.getViaLng());

        log.info("[Estimate] Nearest Nodes: Start={}, Via={}", startNodeId, viaNodeId);

        if (startNodeId == null || viaNodeId == null) {
            return LoopEstimateResponse.builder()
                    .feasible(false)
                    .message("주변에 보행 경로가 없습니다")
                    .build();
        }

        if (straightDist < 0.5) {
            return LoopEstimateResponse.builder()
                    .feasible(false)
                    .straightDistance(straightDist)
                    .message("경유지가 너무 가깝습니다. 최소 500m 이상 떨어뜨려주세요")
                    .build();
        }

        // 최소 루프: P1→P2→P1 왕복
        PathResult path1 = pathfindingService.findShortestPath(startNodeId, viaNodeId);
        PathResult path2 = pathfindingService.findShortestPath(viaNodeId, startNodeId);
        double minLoop = (path1.getTotalDistance() + path2.getTotalDistance()) / 1000.0;

        // 권장 범위 계산
        double recommendedMin = Math.max(minLoop * 1.1, straightDist * 2.5);
        double recommendedMax = straightDist * 7.0;

        log.info("[Estimate] StraightDist={}km, MinLoop={}km, Range={}-{}km",
                straightDist, minLoop, recommendedMin, recommendedMax);

        return LoopEstimateResponse.builder()
                .minLoopDistance(Math.round(minLoop * 100.0) / 100.0)
                .straightDistance(Math.round(straightDist * 100.0) / 100.0)
                .recommendedMin(Math.round(recommendedMin * 100.0) / 100.0)
                .recommendedMax(Math.round(recommendedMax * 100.0) / 100.0)
                .feasible(true)
                .message("루프 경로 생성 가능")
                .build();
    }

    // ==================== 2. Generate: 목표 거리에 맞춰 루프 경로 생성 ====================

    public LoopPathResponse generateLoopPath(LoopPathRequest request) {
        double targetKm = request.getTargetDistanceKm();
        double tolerancePct = request.getTolerancePercent() / 100.0;

        // Step 1: 기본 노드 찾기
        String p1NodeId = findNearestNodeId(request.getStartLat(), request.getStartLng());
        String p2NodeId = findNearestNodeId(request.getViaLat(), request.getViaLng());

        if (p1NodeId == null || p2NodeId == null) {
            return buildFailResponse("주변에 보행 경로가 없습니다");
        }

        double lat1 = request.getStartLat();
        double lon1 = request.getStartLng();
        double lat2 = request.getViaLat();
        double lon2 = request.getViaLng();

        // Step 2: 목표 거리에 맞춰 우회 비율 계산
        double straightDistKm = calculateHaversine(lat1, lon1, lat2, lon2) / 1000.0;
        double deviationFactor = calculateDeviationFactor(straightDistKm, targetKm);

        log.info("[Generate] StraightDist={}km, Target={}km, DeviationFactor={}",
                straightDistKm, targetKm, deviationFactor);

        // Step 3: 우회 지점 A, B 계산
        DeviationPoints dp = calculateDeviationPoints(lat1, lon1, lat2, lon2, deviationFactor);

        String nodeAId = findNearestNodeId(dp.latA, dp.lonA);
        String nodeBId = findNearestNodeId(dp.latB, dp.lonB);

        if (nodeAId == null || nodeBId == null) {
            return buildFailResponse("우회 경유지 주변에 보행 경로가 없습니다");
        }

        log.info("[Generate] 4 Points: P1={}, A={}, P2={}, B={}", p1NodeId, nodeAId, p2NodeId, nodeBId);

        // Step 4: 4개 구간 최단 경로로 연결
        PathResult path1 = pathfindingService.findShortestPath(p1NodeId, nodeAId);
        PathResult path2 = pathfindingService.findShortestPath(nodeAId, p2NodeId);
        PathResult path3 = pathfindingService.findShortestPath(p2NodeId, nodeBId);
        PathResult path4 = pathfindingService.findShortestPath(nodeBId, p1NodeId);

        // 경로 실패 체크
        if (path1.getPath().isEmpty()) return buildFailResponse("출발지 → 우회지A 경로 탐색 실패");
        if (path2.getPath().isEmpty()) return buildFailResponse("우회지A → 중간지점 경로 탐색 실패");
        if (path3.getPath().isEmpty()) return buildFailResponse("중간지점 → 우회지B 경로 탐색 실패");
        if (path4.getPath().isEmpty()) return buildFailResponse("우회지B → 출발지 경로 탐색 실패");

        // Step 5: 전체 경로 합치기
        List<PathNode> fullPath = new ArrayList<>();
        fullPath.addAll(path1.getPath());
        fullPath.addAll(path2.getPath().subList(1, path2.getPath().size()));
        fullPath.addAll(path3.getPath().subList(1, path3.getPath().size()));
        fullPath.addAll(path4.getPath().subList(1, path4.getPath().size()));

        // Step 6: 거리 계산
        double dist1 = path1.getTotalDistance() / 1000.0;
        double dist2 = path2.getTotalDistance() / 1000.0;
        double dist3 = path3.getTotalDistance() / 1000.0;
        double dist4 = path4.getTotalDistance() / 1000.0;
        double totalDistKm = dist1 + dist2 + dist3 + dist4;

        double toleranceKm = Math.abs(totalDistKm - targetKm);
        boolean withinTolerance = toleranceKm <= (targetKm * tolerancePct);

        log.info("[Generate] Loop Complete:");
        log.info("  P1→A: {}km", dist1);
        log.info("  A→P2: {}km", dist2);
        log.info("  P2→B: {}km", dist3);
        log.info("  B→P1: {}km", dist4);
        log.info("  Total: {}km (Target: {}km, Tolerance: {}km, OK: {})",
                totalDistKm, targetKm, toleranceKm, withinTolerance);

        // Step 7: 응답 생성
        LoopPathResponse.SegmentInfo seg1 = LoopPathResponse.SegmentInfo.builder()
                .from("출발지").to("우회지 A")
                .distance(Math.round(dist1 * 100.0) / 100.0)
                .nodeCount(path1.getPath().size())
                .build();

        LoopPathResponse.SegmentInfo seg2 = LoopPathResponse.SegmentInfo.builder()
                .from("우회지 A").to("중간지점")
                .distance(Math.round(dist2 * 100.0) / 100.0)
                .nodeCount(path2.getPath().size())
                .build();

        LoopPathResponse.SegmentInfo seg3 = LoopPathResponse.SegmentInfo.builder()
                .from("중간지점").to("우회지 B")
                .distance(Math.round(dist3 * 100.0) / 100.0)
                .nodeCount(path3.getPath().size())
                .build();

        LoopPathResponse.SegmentInfo seg4 = LoopPathResponse.SegmentInfo.builder()
                .from("우회지 B").to("출발지")
                .distance(Math.round(dist4 * 100.0) / 100.0)
                .nodeCount(path4.getPath().size())
                .build();

        return LoopPathResponse.builder()
                .actualDistance(Math.round(totalDistKm * 100.0) / 100.0)
                .targetDistance(targetKm)
                .tolerance(Math.round(toleranceKm * 100.0) / 100.0)
                .withinTolerance(withinTolerance)
                .path(fullPath)
                .segment1(seg1)
                .segment2(seg2)
                .segment3(seg3)
                .segment4(seg4)
                .message(withinTolerance ?
                        "목표 거리 달성 성공" :
                        String.format("루프 생성 완료 (목표 대비 %.2fkm 차이)", toleranceKm))
                .build();
    }

    private LoopPathResponse buildFailResponse(String message) {
        return LoopPathResponse.builder().message(message).build();
    }

    // ==================== 🔥 핵심: 목표 거리에 맞춰 우회 비율 계산 ====================

    /**
     * 목표 거리에 따라 A, B 지점이 얼마나 멀리 떨어질지 계산
     *
     * 로직:
     * - 직선거리 대비 목표거리 비율로 우회 정도 결정
     * - 목표가 클수록 A, B를 멀리 배치 (타원이 넓어짐)
     * - 목표가 작을수록 A, B를 가깝게 배치 (타원이 좁아짐)
     */
    private double calculateDeviationFactor(double straightDistKm, double targetKm) {
        // 기본 최소 루프 = 직선거리 * 2 (왕복)
        double minLoopKm = straightDistKm * 2.0;

        // 목표가 최소보다 작으면 최소값 사용
        if (targetKm <= minLoopKm) {
            log.warn("[DeviationFactor] Target({}km) <= MinLoop({}km). Using minimum factor.",
                    targetKm, minLoopKm);
            return 0.1; // 최소 우회
        }

        // 추가로 필요한 거리
        double extraDistKm = targetKm - minLoopKm;

        // 우회 비율 계산
        // - extraDist가 작으면 (목표가 최소에 가까움) → factor 작음 (0.2~0.5)
        // - extraDist가 크면 (목표가 최소보다 훨씬 큼) → factor 큼 (0.5~1.5)

        // 비율 = 추가거리 / 직선거리
        double ratio = extraDistKm / straightDistKm;

        // Factor 계산 (0.2 ~ 1.5 범위)
        double factor = 0.2 + (ratio * 0.4);

        // 최대 1.5로 제한 (너무 멀리 떨어지지 않도록)
        factor = Math.min(factor, 1.5);

        log.info("[DeviationFactor] StraightDist={}km, Target={}km, Extra={}km, Ratio={}, Factor={}",
                straightDistKm, targetKm, extraDistKm, ratio, factor);

        return factor;
    }

    // ==================== 우회 지점 계산 ====================

    /**
     * P1-P2 선분의 중점에서 수직 방향으로 떨어진 두 점 A, B 계산
     */
    private DeviationPoints calculateDeviationPoints(
            double lat1, double lon1, double lat2, double lon2, double factor
    ) {
        // 중점 계산
        double midLat = (lat1 + lat2) / 2.0;
        double midLon = (lon1 + lon2) / 2.0;

        // P1→P2 방위각 계산
        double dLon = Math.toRadians(lon2 - lon1);
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double y = Math.sin(dLon) * Math.cos(radLat2);
        double x = Math.cos(radLat1) * Math.sin(radLat2) -
                Math.sin(radLat1) * Math.cos(radLat2) * Math.cos(dLon);
        double bearingRad = Math.atan2(y, x);

        // 수직 방향 (±90도)
        double perpBearingA = bearingRad + (Math.PI / 2);
        double perpBearingB = bearingRad - (Math.PI / 2);

        // P1-P2 거리의 factor 비율만큼 떨어진 위치
        double distP1P2 = calculateHaversine(lat1, lon1, lat2, lon2);
        double deviationDist = distP1P2 * factor;

        // A, B 좌표 계산
        double latA = calculateDestinationLat(midLat, midLon, perpBearingA, deviationDist);
        double lonA = calculateDestinationLon(midLat, midLon, perpBearingA, deviationDist);

        double latB = calculateDestinationLat(midLat, midLon, perpBearingB, deviationDist);
        double lonB = calculateDestinationLon(midLat, midLon, perpBearingB, deviationDist);

        log.info("[Deviation] P1-P2={}m, Factor={}, Deviation={}m", distP1P2, factor, deviationDist);
        log.info("[Deviation] A=({}, {}), B=({}, {})", latA, lonA, latB, lonB);

        DeviationPoints dp = new DeviationPoints();
        dp.latA = latA; dp.lonA = lonA;
        dp.latB = latB; dp.lonB = lonB;
        return dp;
    }

    // ==================== 유틸리티 메서드 ====================

    private String findNearestNodeId(double lat, double lng) {
        String nearestId = null;
        double minDist = Double.MAX_VALUE;

        for (Node node : graphService.getAllNodes()) {
            if (node.getIsVirtual()) continue;

            double dist = calculateHaversine(lat, lng, node.getLatitude(), node.getLongitude());
            if (dist < minDist) {
                minDist = dist;
                nearestId = node.getNodeId();
            }
        }
        return nearestId;
    }

    private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double calculateDestinationLat(double lat, double lon, double bearingRad, double dist) {
        double latRad = Math.toRadians(lat);
        double newLatRad = Math.asin(
                Math.sin(latRad) * Math.cos(dist / R) +
                        Math.cos(latRad) * Math.sin(dist / R) * Math.cos(bearingRad)
        );
        return Math.toDegrees(newLatRad);
    }

    private double calculateDestinationLon(double lat, double lon, double bearingRad, double dist) {
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);
        double newLatRad = Math.asin(
                Math.sin(latRad) * Math.cos(dist / R) +
                        Math.cos(latRad) * Math.sin(dist / R) * Math.cos(bearingRad)
        );

        double newLonRad = lonRad + Math.atan2(
                Math.sin(bearingRad) * Math.sin(dist / R) * Math.cos(latRad),
                Math.cos(dist / R) - Math.sin(latRad) * Math.sin(newLatRad)
        );
        return Math.toDegrees(newLonRad);
    }
}