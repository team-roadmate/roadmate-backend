package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.WalkRouteStartRequest;
import com.trm.roadmate_backend.dto.WalkRouteCompleteRequest;
import com.trm.roadmate_backend.dto.SetCourseRequest;
import com.trm.roadmate_backend.service.WalkRouteService;
import com.trm.roadmate_backend.entity.WalkRoute;
import com.trm.roadmate_backend.dto.common.ApiResponse;
import com.trm.roadmate_backend.config.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(
            summary = "산책 시작",
            description = "예상 거리·시간과 경로 좌표(JSON)를 저장하고 새로운 routeId를 생성합니다."
    )
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Long>> startWalk(
            @RequestBody WalkRouteStartRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long routeId = walkRouteService.startNewRoute(userDetails.getUserId(), request);

        return new ResponseEntity<>(
                ApiResponse.success("산책이 성공적으로 시작되었습니다.", routeId),
                HttpStatus.CREATED
        );
    }

    // 2. PUT /routes/{routeId}/complete : 산책 완료
    @Operation(
            summary = "산책 완료 처리",
            description = "해당 routeId에 대해 실제 이동 거리와 시간을 기록하고 상태를 COMPLETED로 변경합니다."
    )
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
    @Operation(
            summary = "산책 기록 코스 지정",
            description = "완료된 산책 기록에 제목, 메모, 평점을 추가하여 저장된 코스로 지정합니다."
    )
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
    @Operation(
            summary = "코스 지정 해제",
            description = "저장된 코스를 일반 산책 기록 상태로 되돌립니다."
    )
    @PutMapping("/{routeId}/unset-course")
    public ResponseEntity<ApiResponse<Void>> unsetCourse(
            @PathVariable Long routeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.unsetRouteAsCourse(routeId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("코스 지정이 해제되었습니다.", null));
    }

    // 5. GET /routes/history : 전체 기록 조회
    @Operation(
            summary = "전체 산책 기록 조회",
            description = "삭제되지 않은 전체 산책 기록을 조회합니다."
    )
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<WalkRoute>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<WalkRoute> history = walkRouteService.getHistory(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("전체 산책 기록 조회가 완료되었습니다.", history));
    }

    // 6. GET /routes/courses : 저장된 코스 목록 조회
    @Operation(
            summary = "저장된 코스 목록 조회",
            description = "isCourse=true 로 설정된 산책 기록만 조회하여 반환합니다."
    )
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<WalkRoute>>> getSavedCourses(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<WalkRoute> courses = walkRouteService.getSavedCourses(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("저장된 코스 목록 조회가 완료되었습니다.", courses));
    }

    // 7. DELETE /routes/{routeId} : 산책 기록 삭제 (Soft Delete)
    @Operation(
            summary = "산책 기록 삭제 (Soft Delete)",
            description = "실제로 삭제하지 않고 isDeleted=true 로 설정하여 기록을 숨깁니다."
    )
    @DeleteMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(
            @PathVariable Long routeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        walkRouteService.deleteRoute(routeId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("산책 기록이 성공적으로 삭제되었습니다.", null));
    }
}
