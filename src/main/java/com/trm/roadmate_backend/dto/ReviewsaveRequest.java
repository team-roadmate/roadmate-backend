package com.trm.roadmate_backend.dto;


import jakarta.validation.constraints.*;

public record ReviewsaveRequest(

        @NotNull(message = "userId는 필수입니다.")
        Long userId,

        @NotNull(message = "rating은 필수입니다.")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5 이하여야 합니다.")
        Integer rating,

        @Size(max = 2000, message = "댓글은 2000자 이하로 입력해 주세요.")
        String comment,

        @Size(max = 500, message = "이미지 URL은 500자 이하로 입력해 주세요.")
        String imageUrl
) {}