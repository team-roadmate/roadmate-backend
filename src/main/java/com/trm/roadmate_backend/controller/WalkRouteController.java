package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.WalkRouteStartRequest;
import com.trm.roadmate_backend.dto.WalkRouteCompleteRequest;
import com.trm.roadmate_backend.dto.SetCourseRequest;
import com.trm.roadmate_backend.service.WalkRouteService;
import com.trm.roadmate_backend.entity.WalkRoute;
import com.trm.roadmate_backend.dto.common.ApiResponse;
import com.trm.roadmate_backend.config.CustomUserDetails; // ✨ CustomUserDetails import

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/routes")
public class WalkRouteController {

    private final WalkRouteService walkRouteService;

    // 1. POST /routes/start : 산책 시작
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Long>> startWalk(
            @RequestBody WalkRouteStartRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails // ✨ CustomUserDetails에서 userId 추출
    ) {
        // Service에 userId (Integer)를 바로 전달
        Long routeId = walkRouteService.startNewRoute(userDetails.getUserId(), request);

        return new ResponseEntity<>(
                ApiResponse.success("산책이 성공적으로 시작되었습니다.", routeId),
                HttpStatus.CREATED
        );
    }

    // 2. PUT /routes/{routeId}/complete : 산책 완료
    @PutMapping("/{routeId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeWalk(
            @PathVariable Long routeId,
            @RequestBody WalkRouteCompleteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.completeRoute(routeId, userDetails.getUserId(), request);

        return ResponseEntity.ok(ApiResponse.success("산책 기록이 성공적으로 완료되었습니다.", null));
    }

    // 3. PUT /routes/{routeId}/set-course : 코스 지정
    @PutMapping("/{routeId}/set-course")
    public ResponseEntity<ApiResponse<Void>> setCourse(
            @PathVariable Long routeId,
            @RequestBody SetCourseRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.setRouteAsCourse(routeId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("경로가 저장된 코스로 지정되었습니다.", null));
    }

    // 4. PUT /routes/{routeId}/unset-course : 코스 지정 해제
    @PutMapping("/{routeId}/unset-course")
    public ResponseEntity<ApiResponse<Void>> unsetCourse(
            @PathVariable Long routeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.unsetRouteAsCourse(routeId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("코스 지정이 해제되었습니다.", null));
    }

    // 5. GET /routes/history : 전체 기록 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<WalkRoute>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<WalkRoute> history = walkRouteService.getHistory(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("전체 산책 기록 조회가 완료되었습니다.", history));
    }

    // 6. GET /routes/courses : 저장된 코스 목록 조회
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<WalkRoute>>> getSavedCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<WalkRoute> courses = walkRouteService.getSavedCourses(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("저장된 코스 목록 조회가 완료되었습니다.", courses));
    }

    // 7. DELETE /api/routes/{routeId} : 산책 기록 삭제 (Soft Delete)
    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable Long routeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.deleteRoute(routeId, userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success("산책 기록이 성공적으로 삭제되었습니다.", null));
    }
}