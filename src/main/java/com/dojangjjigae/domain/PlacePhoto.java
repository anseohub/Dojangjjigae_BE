package com.dojangjjigae.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacePhoto {
    private String photoId;
    private String url;
    private String photographer;

    @Column(name = "photo_month")
    private String month;
}