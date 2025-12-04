package com.trm.roadmate_backend.controller.walk;

import com.trm.roadmate_backend.dto.common.ApiResponse; // 💡 새로 추가된 ApiResponse
import com.trm.roadmate_backend.dto.walk.RouteLoadResponse;
import com.trm.roadmate_backend.dto.walk.RouteSaveRequest;
import com.trm.roadmate_backend.dto.walk.RouteUpdateRequest;
import com.trm.roadmate_backend.entity.walk.WalkingRoute;
import com.trm.roadmate_backend.service.walk.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/walk-routes")
public class RouteController {

    private final RouteService routeService;

    /**
     * 1. 경로 저장 API: POST /api/walk-routes/save
     * 💡 ApiResponse<Long> 적용 및 201 Created 반환
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Long>> saveRoute(@RequestBody RouteSaveRequest request) {

        WalkingRoute savedRoute = routeService.saveRoute(request);
        Long savedId = savedRoute.getId();

        // 1. ApiResponse 생성 (success: true, data: 저장된 ID)
        ApiResponse<Long> response = ApiResponse.success("경로 저장 성공", savedId);

        // 2. ResponseEntity에 201 Created 상태와 ApiResponse를 담아 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                // 💡 생성된 리소스의 URI를 Location 헤더에 포함하는 REST 표준 방식 사용
                .location(URI.create("/api/walk-routes/" + savedId))
                .body(response);
    }

    /**
     * 2. 경로 조회 API: GET /api/walk-routes/users/{userId}
     * 💡 ApiResponse<List<RouteLoadResponse>> 적용 및 200 OK 반환
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<RouteLoadResponse>>> getRoutesByUserId(@PathVariable Long userId) {

        List<RouteLoadResponse> routes = routeService.getRoutesByUserId(userId);

        if (routes.isEmpty()) {
            // 조회 결과가 없는 경우: 200 OK와 빈 데이터 목록을 반환하거나,
            // 204 No Content를 반환할 수 있으나, 여기서는 일관성을 위해 200 OK를 사용합니다.
            return ResponseEntity.ok(ApiResponse.success("저장된 경로가 없습니다.", routes));
        }

        // 200 OK 상태와 함께 ApiResponse<List<T>>를 반환
        return ResponseEntity.ok(
                ApiResponse.success("경로 목록 조회 성공", routes)
        );
    }

    /**
     * 3. 경로 수정 API: PATCH /api/walk-routes/{routeId}
     * 💡 ApiResponse<Long> 적용 및 200 OK 반환 (Update)
     */
    @PatchMapping("/{routeId}")
    public ResponseEntity<ApiResponse<Long>> updateRoute(
            @PathVariable Long routeId,
            @RequestBody RouteUpdateRequest request) {

        WalkingRoute updatedRoute = routeService.modifyRoute(routeId, request);

        // 200 OK 상태와 함께 ApiResponse<Long> (업데이트된 ID)를 반환
        return ResponseEntity.ok(
                ApiResponse.success("경로 수정 성공", updatedRoute.getId())
        );
    }

    /**
     * 4. 경로 삭제 API: DELETE /api/walk-routes/{routeId}
     * 💡 204 No Content (바디 없음) 반환
     */
    @DeleteMapping("/{routeId}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long routeId) {

        routeService.deleteRoute(routeId);

        // 삭제 성공 시, 본문 없이 204 No Content 반환
        return ResponseEntity.noContent().build();
    }
}