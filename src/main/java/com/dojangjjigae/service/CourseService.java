package com.dojangjjigae.service;

import com.dojangjjigae.domain.Course;
import com.dojangjjigae.dto.CourseDto;
import com.dojangjjigae.repository.CourseRepository;
import com.dojangjjigae.repository.CourseSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;

    /**
     * Main.jsx / Course.jsx 가 쓰던 COURSES 배열을 그대로 대체.
     * "오늘의 추천 코스"나 "다른 추천 코스" 슬라이싱은 프론트가 이미
     * COURSES.slice(...) 로 클라이언트에서 하고 있으므로 백엔드는 전체 목록만 내려주면 된다.
     */
    public List<CourseDto> listAll() {
        return courseRepository.findAll().stream()
                .map(course -> CourseDto.from(course,
                        courseSpotRepository.findByCourseIdOrderBySequenceAsc(course.getId())))
                .toList();
    }

    public CourseDto getBySlug(String slug) {
        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("코스를 찾을 수 없습니다. slug=" + slug));
        var spots = courseSpotRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        return CourseDto.from(course, spots);
    }
}
