package com.dojangjjigae.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 프론트 data/places.js 의 photo 객체와 필드명이 같다.
 * source: 'gallery'(관광사진) | 'tour'(국문 관광정보 대표이미지)
 */
@Getter
@AllArgsConstructor
public class PublicPhotoDto {
    private String source;
    private String id;
    private String url;
    private String photographer;
    private String month;
}
