package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourseProgressDto {

    private String courseId; // slug
    private String title;
    private List<String> chips;
    private int currentStampCount;
    private int totalStampCount;
    /** 로그인 없이 위치도 안 보낸 조회라면 null. 다음 미인증 지점까지의 거리(m) */
    private Double nextSpotDistanceM;
    private String startDateLabel; // "09.02" 형태
    private Double totalDistanceKm;
    private Integer estimatedMinutes;
}
