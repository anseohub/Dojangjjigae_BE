package com.dojangjjigae.dto;

import com.dojangjjigae.domain.Place;
import com.dojangjjigae.domain.PlaceInfo;
import com.dojangjjigae.domain.PlacePhoto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 프론트 data/places.js 의 PLACES 배열 항목과 필드명·모양이 완전히 같다.
 * 프론트는 이 응답 배열을 그대로 PLACES 자리에 채워넣기만 하면 된다.
 */
@Getter
@Builder(toBuilder = true)
public class PlaceDto {

    private String id; // slug
    private String name;
    private String category;
    private boolean indoor;
    private String address;
    private Double lat;
    private Double lng;
    private String contentId;
    private String policy;
    private Integer maxWeightKg;
    private boolean assistanceDogAllowed;
    private List<String> petRules;
    private int stampCount;
    private String summary;
    private String description;
    private InfoDto info;
    private List<PhotoDto> photos;

    @Getter
    @Builder(toBuilder = true)
    public static class InfoDto {
        private String hours;
        private String closed;
        private String parking;
        private String tel;
        private String fee;

        static InfoDto from(PlaceInfo info) {
            if (info == null) {
                return InfoDto.builder().hours("").closed("").parking("").tel("").fee("").build();
            }
            return InfoDto.builder()
                    .hours(nullToEmpty(info.getHours()))
                    .closed(nullToEmpty(info.getClosed()))
                    .parking(nullToEmpty(info.getParking()))
                    .tel(nullToEmpty(info.getTel()))
                    .fee(nullToEmpty(info.getFee()))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PhotoDto {
        private String id;
        private String url;
        private String photographer;
        private String month;

        static PhotoDto from(PlacePhoto photo) {
            return PhotoDto.builder()
                    .id(photo.getPhotoId())
                    .url(photo.getUrl())
                    .photographer(photo.getPhotographer())
                    .month(photo.getMonth())
                    .build();
        }
    }

    public static PlaceDto from(Place place) {
        return PlaceDto.builder()
                .id(place.getSlug())
                .name(place.getName())
                .category(place.getCategory())
                .indoor(place.isIndoor())
                .address(place.getAddress())
                .lat(place.getLat())
                .lng(place.getLng())
                .contentId(nullToEmpty(place.getContentId()))
                .policy(place.getPolicy().name())
                .maxWeightKg(place.getMaxWeightKg())
                .assistanceDogAllowed(place.isAssistanceDogAllowed())
                .petRules(place.getPetRules())
                .stampCount(place.getStampCount())
                .summary(nullToEmpty(place.getSummary()))
                .description(nullToEmpty(place.getDescription()))
                .info(InfoDto.from(place.getInfo()))
                .photos(place.getPhotos().stream().map(PhotoDto::from).toList())
                .build();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
