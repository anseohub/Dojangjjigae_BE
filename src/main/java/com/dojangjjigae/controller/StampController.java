package com.dojangjjigae.controller;

import com.dojangjjigae.dto.*;
import com.dojangjjigae.service.StampService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 로그인이 없으므로 userId 를 받지 않고, 서버 내부에서 고정 테스트 유저로 처리한다. */
@RestController
@RequiredArgsConstructor
public class StampController {

    private final StampService stampService;

    /** Course.jsx "이 코스로 시작하기" 버튼 */
    @PostMapping("/api/course-progress/start")
    public void start(@Valid @RequestBody StartCourseRequest request) {
        stampService.startCourse(request);
    }

    /** Stamp.jsx 상단 진행중인 코스 카드 */
    @GetMapping("/api/course-progress/current")
    public CourseProgressDto currentProgress(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return stampService.getCurrentProgress(lat, lng);
    }

    /** Stamp.jsx 스탬프 지점 목록 (인증완료/지금인증가능/범위밖) */
    @GetMapping("/api/courses/{courseSlug}/spots")
    public List<SpotStatusDto> spots(
            @PathVariable String courseSlug,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        return stampService.getSpotsStatus(courseSlug, lat, lng);
    }

    /** 스탬프 찍기 전 반경 100m 확인 (contentId 기준, 37개 장소 공통) */
    @PostMapping("/api/stamps/check")
    public StampCheckResponse check(@Valid @RequestBody StampCheckRequest request) {
        return stampService.check(request);
    }

    /** 핵심: GPS 인증(스탬프 찍기) */
    @PostMapping("/api/stamps/certify")
    public CertifyResponse certify(@Valid @RequestBody CertifyRequest request) {
        return stampService.certify(request);
    }
}
