package com.dojangjjigae.controller;

import com.dojangjjigae.dto.CourseDto;
import com.dojangjjigae.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /** Main.jsx / Course.jsx 가 쓰던 COURSES 배열을 그대로 대체 */
    @GetMapping
    public List<CourseDto> listAll() {
        return courseService.listAll();
    }

    @GetMapping("/{slug}")
    public CourseDto getBySlug(@PathVariable String slug) {
        return courseService.getBySlug(slug);
    }
}
