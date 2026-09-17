package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** GET /api/public/places/{contentId} 응답 (data/places.js 의 mergePublicData 가 그대로 파싱함) */
@Getter
@Builder
public class PublicPlaceResponse {
    private Map<String, String> status;
    private String address;
    private Double lat;
    private Double lng;
    private List<String> description;
    private List<String> petRules;
    private Map<String, List<String>> info;
    private List<PublicPhotoDto> photos;
    private List<String> sources;
}
