package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자가 특정 코스의 특정 지점에서 스탬프를 찍은 기록.
 * Day 3 에서 /api/stamps/certify (Haversine 거리 계산) 와 함께 구현.
 */
@Entity
@Table(name = "stamp_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /** null 이면 아직 미인증 */
    private LocalDateTime certifiedAt;
}
