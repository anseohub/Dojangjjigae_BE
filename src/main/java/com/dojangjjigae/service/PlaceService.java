package com.dojangjjigae.service;

import com.dojangjjigae.domain.Place;
import com.dojangjjigae.dto.PlaceDto;
import com.dojangjjigae.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final TourApiClient tourApiClient;

    /** Explore.jsx 의 PLACES 배열을 그대로 대체 */
    public List<PlaceDto> listAll() {
        return placeRepository.findAll().stream().map(PlaceDto::from).toList();
    }

    /**
     * 장소 상세 조회. contentId 가 있는 장소(TourAPI 연동 장소)는
     * 매 요청마다 TourAPI 를 실시간으로 호출해 overview/전화번호를 보강한다.
     * 큐레이션한 기존 값이 이미 있으면 덮어쓰지 않는다.
     */
    public PlaceDto getBySlug(String slug) {
        Place place = placeRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("장소를 찾을 수 없습니다. slug=" + slug));

        PlaceDto baseDto = PlaceDto.from(place);

        if (place.getContentId() == null || place.getContentId().isBlank()) {
            return baseDto;
        }

        PlaceDto enriched = tourApiClient.fetchDetail(place.getContentId())
                .map(detail -> enrich(baseDto, detail))
                .orElse(baseDto);

        // 큐레이션된 사진이 없는 장소만 detailImage2 로 실시간 보강
        if (enriched.getPhotos().isEmpty()) {
            enriched = enrichPhotos(enriched, place.getContentId());
        }

        return enriched;
    }

    private PlaceDto enrichPhotos(PlaceDto dto, String contentId) {
        List<String> livePhotos = tourApiClient.fetchPhotos(contentId);
        if (livePhotos.isEmpty()) {
            return dto;
        }

        List<PlaceDto.PhotoDto> photos = livePhotos.stream()
                .map(url -> PlaceDto.PhotoDto.builder().id(url).url(url).photographer("").month("").build())
                .toList();

        return dto.toBuilder().photos(photos).build();
    }
    private PlaceDto enrich(PlaceDto dto, TourApiClient.TourApiDetail detail) {
        String description = dto.getDescription().isBlank() ? detail.overview() : dto.getDescription();
        String tel = dto.getInfo().getTel().isBlank() ? detail.tel() : dto.getInfo().getTel();

        return dto.toBuilder()
                .description(description)
                .info(dto.getInfo().toBuilder().tel(tel).build())
                .build();
    }
}
