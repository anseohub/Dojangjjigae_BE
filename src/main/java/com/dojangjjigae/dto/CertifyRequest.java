package com.dojangjjigae.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertifyRequest {

    @NotBlank
    private String courseSlug;

    @NotBlank
    private String placeSlug;

    @NotNull
    private Double lat;

    @NotNull
    private Double lng;
}
