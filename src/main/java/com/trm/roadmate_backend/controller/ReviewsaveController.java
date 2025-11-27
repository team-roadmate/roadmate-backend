package com.trm.roadmate_backend.controller;

import com.trm.roadmate_backend.dto.ReviewsaveRequest;
import com.trm.roadmate_backend.dto.ReviewsaveResponse;
import com.trm.roadmate_backend.service.ReviewsaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewsaveController {

    private final ReviewsaveService reviewService;

    /**

    특정 코스에 대한 리뷰 저장 (완주 + 평가)
    POST /api/routes/{routeId}/reviews
    */
    @PostMapping("/routes/{routeId}/reviews")
    public ResponseEntity<ReviewsaveResponse> createReview(@PathVariable Long routeId,@Valid @RequestBody ReviewsaveRequest request) {
        return ResponseEntity.ok(reviewService.createReview(routeId, request));}

    /**

            (옵션) 코스별 리뷰 리스트 조회
    GET /api/routes/{routeId}/reviews
     */
    @GetMapping("/routes/{routeId}/reviews")
    public ResponseEntity<List<ReviewsaveResponse>> getReviewsByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(reviewService.getReviewsByRoute(routeId));}

    /**

     (옵션) 유저별 리뷰 리스트 조회
     GET /api/users/{userId}/reviews*/@GetMapping("/users/{userId}/reviews")
    public ResponseEntity<List<ReviewsaveResponse>> getReviewsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getReviewsByUser(userId));}
}