package com.trm.roadmate_backend.service;


import com.trm.roadmate_backend.dto.SetCourseRequest;
import com.trm.roadmate_backend.dto.WalkRouteCompleteRequest;
import com.trm.roadmate_backend.dto.WalkRouteStartRequest;
import com.trm.roadmate_backend.entity.RouteStatus;
import com.trm.roadmate_backend.entity.WalkRoute;
import com.trm.roadmate_backend.repository.WalkRouteRepository;
import com.trm.roadmate_backend.exception.UnauthorizedUserException; // ✨ 예외 import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkRouteService {

    private final WalkRouteRepository walkRouteRepository;
    // private final UserRepository userRepository; // ❌ 불필요해짐

    // ❌ Email을 User ID로 변환하는 getUserIdFromEmail 메서드 제거

    @Transactional
    // Integer userId를 직접 받도록 변경
    public Long startNewRoute(Integer userId, WalkRouteStartRequest request) {
        // JWT 필터에서 이미 인증되었으므로, userId가 유효하다고 간주하고 바로 로직 수행

        WalkRoute newRoute = WalkRoute.builder()
                .userId(userId)
                .expectedDistance(request.getExpectedDistance())
                .expectedDuration(request.getExpectedDuration())
                .pathData(request.getPathData())
                .status(RouteStatus.STARTED)
                .startTime(LocalDateTime.now())
                .build();

        WalkRoute savedRoute = walkRouteRepository.save(newRoute);
        return savedRoute.getRouteId();
    }

    @Transactional
    // Integer userId를 직접 받도록 변경
    public void completeRoute(Long routeId, Integer userId, WalkRouteCompleteRequest request) {
        // userId를 이용해 소유권 검증 및 조회
        WalkRoute route = walkRouteRepository.findByRouteIdAndUserId(routeId, userId)
                .orElseThrow(() -> new UnauthorizedUserException("경로를 찾을 수 없거나 해당 경로에 대한 접근 권한이 없습니다.")); // ✨ 예외 변경

        if (route.getStatus() == RouteStatus.COMPLETED) {
            throw new RuntimeException("이미 완료된 경로입니다.");
        }

        route.complete(request.getDistance(), request.getDuration());
    }

    @Transactional
    public void setRouteAsCourse(Long routeId, Integer userId, SetCourseRequest request) {
        WalkRoute route = walkRouteRepository.findByRouteIdAndUserId(routeId, userId)
                .orElseThrow(() -> new UnauthorizedUserException("경로를 찾을 수 없거나 해당 경로에 대한 접근 권한이 없습니다.")); // ✨ 예외 변경

        if (route.getStatus() != RouteStatus.COMPLETED) {
            throw new RuntimeException("완료된 경로만 코스로 지정 가능합니다.");
        }

        route.setAsCourse(request.getTitle(), request.getMemo(), request.getRating());
    }

    @Transactional
    public void unsetRouteAsCourse(Long routeId, Integer userId) {
        WalkRoute route = walkRouteRepository.findByRouteIdAndUserId(routeId, userId)
                .orElseThrow(() -> new UnauthorizedUserException("경로를 찾을 수 없거나 해당 경로에 대한 접근 권한이 없습니다.")); // ✨ 예외 변경

        route.unSetAsCourse();
    }

    @Transactional
    public void deleteRoute(Long routeId, Integer userId) {
        WalkRoute route = walkRouteRepository.findByRouteIdAndUserId(routeId, userId)
                .orElseThrow(() -> new UnauthorizedUserException("경로를 찾을 수 없거나 해당 경로에 대한 접근 권한이 없습니다."));

        // 경로 삭제 처리 (소프트 딜리트)
        route.delete();
    }

    /**
     * 특정 routeId의 산책 기록을 조회합니다. (소유권 검증 포함)
     * @param routeId 조회할 경로 ID
     * @param userId 현재 사용자 ID
     * @return WalkRoute 엔티티
     * @throws UnauthorizedUserException 해당 경로를 찾을 수 없거나 접근 권한이 없을 경우
     */
    public WalkRoute getRouteById(Long routeId, Integer userId) {
        // routeId와 userId를 모두 사용하여 경로를 찾고, isDeleted=false인 경우만 가져옴
        return walkRouteRepository.findByRouteIdAndUserIdAndIsDeleted(routeId, userId, false)
                .orElseThrow(() -> new UnauthorizedUserException("해당 경로를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    public List<WalkRoute> getHistory(Integer userId) {
        return walkRouteRepository.findByUserIdAndIsDeletedOrderByStartTimeDesc(userId, false);
    }

    public List<WalkRoute> getSavedCourses(Integer userId) {
        return walkRouteRepository.findByUserIdAndIsCourseAndIsDeletedOrderByStartTimeDesc(userId, true, false);
    }
}