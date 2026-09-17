package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** 사용자가 "이 코스로 시작하기"를 눌러 시작한 코스. 최신 1건이 "진행중인 코스"가 된다. */
@Entity
@Table(name = "user_course_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDateTime startedAt;
}
