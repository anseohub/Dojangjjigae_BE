package com.dojangjjigae.repository;

import com.dojangjjigae.domain.UserCourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCourseProgressRepository extends JpaRepository<UserCourseProgress, Long> {
    Optional<UserCourseProgress> findFirstByUserIdOrderByStartedAtDesc(Long userId);
}
