package com.dojangjjigae.controller;

import com.dojangjjigae.dto.PublicPhotosResponse;
import com.dojangjjigae.dto.PublicPlaceResponse;
import com.dojangjjigae.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 프론트 data/places.js(loadPlacesPublicData) + utils/publicData.js 가 실제로 호출하는 엔드포인트.
 * Explore.jsx / PlaceDetail.jsx 는 이 경로의 응답으로 사진 · 주소 · 개요를 채운다.
 * /api/places(PlaceController)는 현재 화면에서 쓰이지 않는 별개의 경로다.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataService publicDataService;

    /** contentId가 없는 장소: 관광사진 갤러리에서만 사진을 찾는다 */
    @GetMapping("/photos")
    public PublicPhotosResponse photos(
            @RequestParam(required = false) String galleryIds,
            @RequestParam(required = false) String keywords) {
        return publicDataService.resolvePhotos(split(galleryIds), split(keywords));
    }

    /** contentId가 있는 장소: 공통정보 + 사진(대표이미지 + 관광사진 갤러리)을 함께 채운다. petTour=Y면 반려동물 동반여행 정보도 채운다 */
    @GetMapping("/places/{contentId}")
    public PublicPlaceResponse place(
            @PathVariable String contentId,
            @RequestParam(required = false) String galleryIds,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String petTour) {
        return publicDataService.resolvePlace(contentId, split(galleryIds), split(keywords), "Y".equalsIgnoreCase(petTour));
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
