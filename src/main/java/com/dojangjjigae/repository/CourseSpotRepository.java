package com.dojangjjigae.repository;

import com.dojangjjigae.domain.CourseSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseSpotRepository extends JpaRepository<CourseSpot, Long> {
    List<CourseSpot> findByCourseIdOrderBySequenceAsc(Long courseId);
}
