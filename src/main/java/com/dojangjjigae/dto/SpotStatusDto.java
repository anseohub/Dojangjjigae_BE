package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SpotStatusDto {

    public enum Status { CERTIFIED, AVAILABLE, OUT_OF_RANGE }

    private int sequence;
    private String placeId; // slug
    private String name;
    private String imageUrl;
    private List<String> tags;
    private Status status;
    private String certifiedAtLabel; // "09.04 14:20 인증" 형태, 미인증이면 null
    private Double distanceM;        // lat/lng 를 보냈을 때만 채워짐
}
