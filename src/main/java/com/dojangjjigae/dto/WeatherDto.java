package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 프론트 data/weather.js 의 날씨 형식과 필드명이 같다 (classifyWeather 가 그대로 판정).
 * sky · pty 는 기상청 코드 체계로 맞춰서 보낸다.
 */
@Getter
@Builder
public class WeatherDto {
    private LocalDate date;
    private int sky;          // 1 맑음 · 3 구름많음 · 4 흐림
    private int pty;          // 0 없음 · 1 비 · 2 비/눈 · 3 눈 · 4 소나기 · 5 빗방울
    private double temp;      // 현재 기온(°C)
    private Double tmx;       // 오늘 최고 기온(°C)
    private int pop;          // 강수확률(%) · 다음 3시간 예보 기준
    private double wind;      // 풍속(m/s)
    private Integer pm10;     // 미세먼지(㎍/㎥), 못 받으면 null
    private String condition; // OpenWeather 한글 설명 (예: 튼구름)
    private String source;    // openweather · fallback(키 없음 또는 호출 실패)
}
