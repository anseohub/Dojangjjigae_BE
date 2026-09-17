package com.dojangjjigae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 스탬프 찍기 전 반경 확인 요청 (장소는 TourAPI contentId로 찾는다) */
@Getter
@Setter
public class StampCheckRequest {

    @NotBlank
    private String contentId;

    @NotNull
    private Double lat;

    @NotNull
    private Double lng;
}
