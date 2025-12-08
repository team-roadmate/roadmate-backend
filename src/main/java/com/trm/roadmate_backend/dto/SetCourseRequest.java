package com.trm.roadmate_backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetCourseRequest {
    private String title;
    private String memo;
    private Integer rating;
}