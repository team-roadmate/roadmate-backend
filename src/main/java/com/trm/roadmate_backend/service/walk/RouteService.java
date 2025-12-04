package com.trm.roadmate_backend.service.walk;

import com.trm.roadmate_backend.dto.walk.Coordinate;
import com.trm.roadmate_backend.dto.walk.RouteLoadResponse;
import com.trm.roadmate_backend.dto.walk.RouteSaveRequest;
import com.trm.roadmate_backend.dto.walk.RouteUpdateRequest;
import com.trm.roadmate_backend.entity.walk.WalkingRoute;
import com.trm.roadmate_backend.repository.walk.WalkingRouteRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final WalkingRouteRepository routeRepository;
    private final ObjectMapper objectMapper;

    private final TypeReference<List<Coordinate>> coordinateListType =
            new TypeReference<List<Coordinate>>() {};

    /**
     * 경로 저장 로직 (Node ID 제거됨)
     */
    @Transactional
    public WalkingRoute saveRoute(RouteSaveRequest request) {
        try {
            // 1. 좌표 리스트를 JSON 문자열로 직렬화(Serialization)
            String pathCoordinatesJson = objectMapper.writeValueAsString(request.pathCoordinates());

            // 2. Entity 생성 및 초기값 설정
            WalkingRoute route = WalkingRoute.builder()
                    .userId(request.userId())
                    .title(request.title())
                    .userMemo(request.userMemo())

                    .pathCoordinatesJson(pathCoordinatesJson)

                    .totalDistance(request.totalDistance())
                    .durationSeconds(request.durationSeconds())
                    .savedAt(LocalDateTime.now())
                    .isCompleted(false)
                    .developerRating(null)
                    .build();

            // 3. DB 저장
            return routeRepository.save(route);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("경로 좌표 데이터를 JSON으로 변환하는 데 실패했습니다.", e);
        }
    }

    /**
     * 특정 사용자의 모든 도보 경로를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<RouteLoadResponse> getRoutesByUserId(Long userId) {
        // (Repository는 변경 없음, 메소드 이름 그대로 사용)
        List<WalkingRoute> routes = routeRepository.findByUserIdOrderBySavedAtDesc(userId);

        return routes.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 경로 수정 로직 (메타데이터 업데이트)
     */
    @Transactional
    public WalkingRoute modifyRoute(Long routeId, RouteUpdateRequest request) {
        // 1. 경로 ID로 엔티티 조회 (없으면 예외 발생)
        WalkingRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new NoSuchElementException("ID가 " + routeId + "인 경로를 찾을 수 없습니다."));

        // 2. 엔티티 내부 update 메서드 호출 (JPA 더티 체킹 활용)
        route.update(request);

        // @Transactional에 의해 메서드 종료 시 변경 내용이 DB에 자동 반영됩니다.
        return route;
    }

    /**
     * 경로 삭제 로직
     */
    @Transactional
    public void deleteRoute(Long routeId) {
        // 1. 경로 존재 여부 확인 (없으면 예외 발생)
        if (!routeRepository.existsById(routeId)) {
            throw new NoSuchElementException("ID가 " + routeId + "인 경로를 찾을 수 없습니다.");
        }

        // 2. 경로 삭제
        routeRepository.deleteById(routeId);
    }

    /**
     * 단일 WalkingRoute 엔티티를 RouteLoadResponse DTO로 변환
     */
    private RouteLoadResponse convertToResponseDto(WalkingRoute route) {
        List<Coordinate> coordinates;
        try {
            // 💡 JSON 문자열을 List<Coordinate> 객체로 역직렬화(Deserialization)
            coordinates = objectMapper.readValue(
                    route.getPathCoordinatesJson(),
                    coordinateListType
            );
        } catch (JsonProcessingException e) {
            System.err.println("경로 ID " + route.getId() + "의 JSON 데이터가 유효하지 않습니다: " + e.getMessage());
            coordinates = List.of();
        }

        // Node ID 필드가 제거된 DTO 사용
        return RouteLoadResponse.from(route, coordinates);
    }
}