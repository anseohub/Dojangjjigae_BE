package com.dojangjjigae.repository;

import com.dojangjjigae.domain.StampRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StampRecordRepository extends JpaRepository<StampRecord, Long> {

    List<StampRecord> findByUserIdAndCourseId(Long userId, Long courseId);

    Optional<StampRecord> findByUserIdAndCourseIdAndPlaceId(Long userId, Long courseId, Long placeId);

    long countByUserIdAndCourseIdAndCertifiedAtIsNotNull(Long userId, Long courseId);
}
