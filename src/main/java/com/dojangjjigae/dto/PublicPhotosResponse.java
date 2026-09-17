package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** GET /api/public/photos 응답 (utils/publicData.js 가 그대로 파싱함) */
@Getter
@Builder
public class PublicPhotosResponse {
    private Map<String, String> status;
    private List<PublicPhotoDto> photos;
    private List<String> sources;
}
