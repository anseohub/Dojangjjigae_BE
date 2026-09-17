package com.dojangjjigae.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

/** 프론트 place.info = { hours, closed, parking, tel, fee } 와 1:1 대응. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceInfo {
    private String hours;
    private String closed;
    private String parking;
    private String tel;
    private String fee;
}
