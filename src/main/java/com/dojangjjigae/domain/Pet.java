package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Profile.jsx 등록 폼과 대응.
 * Day 2 에서 PetRepository / PetService / PetController 를 붙일 예정.
 */
@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인을 단순화했으므로 지금은 고정 테스트 유저 id(1L)를 사용 */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    private String photoUrl;
    private String breed;
    private Integer birthYear;
    private Integer birthMonth;

    @Enumerated(EnumType.STRING)
    private PetSize size;

    private boolean female;
    private boolean neutered;

    /** 예: ["온순", "차 이동 가능"] */
    @ElementCollection
    @CollectionTable(name = "pet_temperament_tags", joinColumns = @JoinColumn(name = "pet_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> temperamentTags = new ArrayList<>();

    @Column(length = 200)
    private String memo;
}
