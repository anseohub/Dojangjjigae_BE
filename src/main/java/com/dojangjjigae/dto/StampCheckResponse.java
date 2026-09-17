package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StampCheckResponse {
    private boolean withinRadius;
    private Double distanceM;
    private int radiusM;
    private String message;
}
