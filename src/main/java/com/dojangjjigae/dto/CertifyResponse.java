package com.dojangjjigae.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CertifyResponse {
    private boolean certified;
    private double distanceM;
    private int currentStampCount;
    private int totalStampCount;
    private String message;
}
