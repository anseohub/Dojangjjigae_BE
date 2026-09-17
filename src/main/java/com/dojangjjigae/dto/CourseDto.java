package com.dojangjjigae.dto;

import com.dojangjjigae.domain.Course;
import com.dojangjjigae.domain.CourseSpot;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 프론트 data/courses.js 의 COURSES 배열 항목과 필드명·모양이 완전히 같다.
 * placeIds 는 코스에 속한 장소들의 slug 를 방문 순서대로 나열한 배열이다.
 */
@Getter
@Builder
public class CourseDto {

    private String id; // slug
    private String title;
    private String type;
    private List<String> placeIds;
    private String distance;
    private String duration;
    private List<String> desc;

    public static CourseDto from(Course course, List<CourseSpot> orderedSpots) {
        return CourseDto.builder()
                .id(course.getSlug())
                .title(course.getTitle())
                .type(course.getType())
                .placeIds(orderedSpots.stream().map(cs -> cs.getPlace().getSlug()).toList())
                .distance(course.getDistance())
                .duration(course.getDuration())
                .desc(course.getDesc())
                .build();
    }
}
