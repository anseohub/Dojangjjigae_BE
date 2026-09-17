package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 프론트 data/places.js 의 PLACES 배열 항목 하나와 필드가 그대로 대응된다.
 * id 대신 slug(문자열)를 외부 식별자로 쓴다 — 프론트가 'gakwonsa' 같은 문자열 id를 쓰기 때문.
 */
@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 내부 PK. 프론트에는 노출하지 않고 slug 를 노출한다.

    @Column(unique = true, nullable = false)
    private String slug; // 프론트의 place.id (예: "gakwonsa")

    @Column(nullable = false)
    private String name;

    /** 프론트 category 는 "문화 · 역사" 같은 한글 라벨 그대로라 enum 대신 문자열로 관리 */
    @Column(nullable = false)
    private String category;

    private boolean indoor;

    @Column(nullable = false)
    private String address;

    private Double lat;
    private Double lng;

    /** 한국관광공사 TourAPI contentid (없으면 빈 문자열) */
    private String contentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetPolicy policy;

    private Integer maxWeightKg;

    @Builder.Default
    private boolean assistanceDogAllowed = true;

    @ElementCollection
    @CollectionTable(name = "place_pet_rules", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "rule")
    @OrderColumn(name = "rule_order")
    @Builder.Default
    private List<String> petRules = new ArrayList<>();

    @Builder.Default
    private int stampCount = 1;

    @Column(length = 1000)
    private String summary;

    @Column(length = 2000)
    private String description;

    @Embedded
    private PlaceInfo info;

    @ElementCollection
    @CollectionTable(name = "place_photos", joinColumns = @JoinColumn(name = "place_id"))
    @OrderColumn(name = "photo_order")
    @Builder.Default
    private List<PlacePhoto> photos = new ArrayList<>();
}
