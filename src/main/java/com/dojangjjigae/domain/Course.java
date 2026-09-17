package com.dojangjjigae.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 프론트 data/courses.js 의 COURSES 배열 항목 하나와 대응.
 * placeIds 순서는 CourseSpot(course_id, place_id, sequence) 테이블로 관리하고,
 * DTO 변환 시 순서대로 slug 리스트로 펼쳐서 내려준다.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug; // 프론트의 course.id (예: "gwangdeok-nature")

    @Column(nullable = false)
    private String title;

    /** "야외" | "실내" | "야외 · 실내" - 프론트 course.type 그대로 */
    @Column(nullable = false)
    private String type;

    /** 아직 실측 전이면 빈 문자열 ("") - 프론트도 같은 방식으로 비워둠 */
    @Builder.Default
    private String distance = "";

    @Builder.Default
    private String duration = "";

    /** 프론트 course.desc 는 길이 2인 문자열 배열 */
    @ElementCollection
    @CollectionTable(name = "course_desc", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "line")
    @OrderColumn(name = "line_order")
    @Builder.Default
    private List<String> desc = new ArrayList<>();
}
