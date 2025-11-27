package com.trm.roadmate_backend.service;

import com.trm.roadmate_backend.dto.ReviewsaveRequest;
import com.trm.roadmate_backend.dto.ReviewsaveResponse;
import com.trm.roadmate_backend.entity.ReviewSave;
import com.trm.roadmate_backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@RequiredArgsConstructor
public class ReviewsaveService {

    private final ReviewRepository reviewRepository;

    // 리뷰 저장 (완주한 코스 + 평가)
    public ReviewsaveResponse createReview(Long routeId, ReviewsaveRequest request) {

        ReviewSave review = ReviewSave.builder()
                .userId(request.userId())
                .routeId(routeId)
                .rating(request.rating())
                .comment(request.comment())
                .imageUrl(request.imageUrl())
                .createdAt(LocalDateTime.now())
                .build();

        ReviewSave saved = reviewRepository.save(review);

        return new ReviewsaveResponse(
                saved.getReviewId(),
                saved.getUserId(),
                saved.getRouteId(),
                saved.getRating(),
                saved.getComment(),
                saved.getImageUrl(),
                saved.getCreatedAt()
        );
    }
    // (옵션) 코스별 리뷰 목록
    public List<ReviewsaveResponse> getReviewsByRoute(Long routeId) {
        return reviewRepository.findByRouteId(routeId).stream()
                .map(r -> new ReviewsaveResponse(
                        r.getReviewId(),
                        r.getUserId(),
                        r.getRouteId(),
                        r.getRating(),
                        r.getComment(),
                        r.getImageUrl(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    // (옵션) 유저별 리뷰 목록
    public List<ReviewsaveResponse> getReviewsByUser(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(r -> new ReviewsaveResponse(
                        r.getReviewId(),
                        r.getUserId(),
                        r.getRouteId(),
                        r.getRating(),
                        r.getComment(),
                        r.getImageUrl(),
                        r.getCreatedAt()
                ))
                .toList();
    }
}
