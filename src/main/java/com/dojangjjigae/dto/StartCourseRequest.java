package com.dojangjjigae.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartCourseRequest {
    @NotBlank
    private String courseSlug;
}
