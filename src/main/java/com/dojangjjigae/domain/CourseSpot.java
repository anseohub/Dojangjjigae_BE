package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

/** 코스 하나에 속한 장소들의 방문 순서. Course : Place = N : M 을 순서와 함께 표현. */
@Entity
@Table(name = "course_spots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false)
    private Integer sequence;
}
