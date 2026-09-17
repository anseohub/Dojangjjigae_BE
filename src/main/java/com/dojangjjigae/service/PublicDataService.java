package com.dojangjjigae.service;

import com.dojangjjigae.dto.PublicPhotoDto;
import com.dojangjjigae.dto.PublicPhotosResponse;
import com.dojangjjigae.dto.PublicPlaceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 프론트가 실제로 렌더링에 쓰는 data/places.js + utils/publicData.js 가 기대하는
 * /api/public/photos, /api/public/places/{contentId} 응답을 만드는 서비스.
 *
 * /api/places(PlaceController/PlaceService)와는 완전히 별개의 경로다 — 저쪽은
 * CuratedDataSeeder가 만든 DB 레코드를 쓰지만, 이쪽은 프론트의 CURATED_PLACES(data/places.js)를
 * 기준으로 매 요청마다 TourAPI를 실시간 호출해 사진 · 주소 · 개요를 채워준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataService {

    private static final Pattern LEADING_DIGITS = Pattern.compile("^(\\d+)");
    private static final String SOURCE_NAME = "한국관광공사";

    private final TourApiClient tourApiClient;

    /** GET /api/public/photos?galleryIds=&keywords= — contentId가 없는 장소용 (관광사진 갤러리만 검색) */
    public PublicPhotosResponse resolvePhotos(List<String> galleryIds, List<String> keywords) {
        List<PublicPhotoDto> photos = resolveGalleryPhotos(galleryIds, keywords);
        List<String> sources = photos.isEmpty() ? List.of() : List.of(SOURCE_NAME);
        return PublicPhotosResponse.builder()
                .status(Map.of("photos", "ok"))
                .photos(photos)
                .sources(sources)
                .build();
    }

    /** GET /api/public/places/{contentId}?galleryIds=&keywords= — contentId가 있는 장소용 */
    public PublicPlaceResponse resolvePlace(String contentId, List<String> galleryIds, List<String> keywords, boolean petTour) {
        Map<String, String> status = new LinkedHashMap<>();
        List<PublicPhotoDto> photos = new ArrayList<>();
        boolean anySuccess = false;

        String address = null;
        Double lat = null;
        Double lng = null;
        List<String> description = List.of();
        String contentTypeId = null;

        Optional<TourApiClient.CommonDetail> common = tourApiClient.fetchCommon(contentId);
        if (common.isPresent()) {
            TourApiClient.CommonDetail detail = common.get();
            address = detail.address();
            lat = detail.lat();
            lng = detail.lng();
            description = (detail.overview() == null || detail.overview().isBlank())
                    ? List.of() : List.of(detail.overview());
            contentTypeId = detail.contentTypeId();
            photos.addAll(toTourPhotos(detail.images()));
            status.put("common", "ok");
            anySuccess = true;
        } else {
            status.put("common", "error");
        }

        // detailImage2 로 대표이미지 외 추가 사진 보강
        photos.addAll(toTourPhotos(tourApiClient.fetchPhotos(contentId)));

        List<PublicPhotoDto> galleryPhotos = resolveGalleryPhotos(galleryIds, keywords);
        photos.addAll(galleryPhotos);
        if (!galleryPhotos.isEmpty()) anySuccess = true;
        status.put("photos", "ok");

        // 반려동물 동반여행 API는 프론트에서 petTour=Y로 요청한 장소만 호출한다 (나머지는 '정보 없음')
        List<String> petRules = null;
        Map<String, List<String>> info = null;
        status.put("intro", "error");
        status.put("pet", "error");

        if (petTour) {
            Optional<List<String>> pet = tourApiClient.fetchPetTour(contentId);
            if (pet.isPresent()) {
                petRules = pet.get();
                status.put("pet", "ok");
                anySuccess = true;
            }

            Optional<TourApiClient.PetIntro> intro = tourApiClient.fetchPetIntro(contentId, contentTypeId);
            if (intro.isPresent()) {
                TourApiClient.PetIntro value = intro.get();
                info = new LinkedHashMap<>();
                info.put("hours", value.hours());
                info.put("closed", value.closed());
                info.put("parking", value.parking());
                info.put("fee", value.fee());
                status.put("intro", "ok");
                anySuccess = true;
            }
        }

        List<String> sources = anySuccess ? List.of(SOURCE_NAME) : List.of();

        return PublicPlaceResponse.builder()
                .status(status)
                .address(address)
                .lat(lat)
                .lng(lng)
                .description(description)
                .petRules(petRules)
                .info(info)
                .photos(dedupePhotos(photos))
                .sources(sources)
                .build();
    }

    private List<PublicPhotoDto> resolveGalleryPhotos(List<String> galleryIds, List<String> keywords) {
        if (galleryIds == null || galleryIds.isEmpty()) return List.of();
        Set<String> wanted = new LinkedHashSet<>(galleryIds);
        Map<String, PublicPhotoDto> found = new LinkedHashMap<>();

        if (keywords != null) {
            for (String keyword : keywords) {
                if (found.keySet().containsAll(wanted)) break;
                for (TourApiClient.GalleryPhoto photo : tourApiClient.fetchGalleryByKeyword(keyword)) {
                    if (wanted.contains(photo.contentId()) && !found.containsKey(photo.contentId())) {
                        found.put(photo.contentId(), new PublicPhotoDto(
                                "gallery", photo.contentId(), photo.url(), photo.photographer(), photo.month()));
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    private List<PublicPhotoDto> toTourPhotos(List<String> urls) {
        List<PublicPhotoDto> result = new ArrayList<>();
        for (String url : urls) {
            String id = extractImageId(url);
            if (id == null) continue;
            result.add(new PublicPhotoDto("tour", id, url, "", ""));
        }
        return result;
    }

    private List<PublicPhotoDto> dedupePhotos(List<PublicPhotoDto> photos) {
        Map<String, PublicPhotoDto> byKey = new LinkedHashMap<>();
        for (PublicPhotoDto photo : photos) {
            byKey.putIfAbsent(photo.getSource() + ":" + photo.getId(), photo);
        }
        return new ArrayList<>(byKey.values());
    }

    /** 이미지 URL의 파일명 맨 앞 숫자를 사진 id로 쓴다 (예: .../86/3488286_image2_1.JPG → 3488286) */
    private String extractImageId(String url) {
        if (url == null || url.isBlank()) return null;
        String last = url.substring(url.lastIndexOf('/') + 1);
        Matcher matcher = LEADING_DIGITS.matcher(last);
        return matcher.find() ? matcher.group(1) : null;
    }
}
