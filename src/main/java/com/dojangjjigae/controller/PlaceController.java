package com.dojangjjigae.controller;

import com.dojangjjigae.dto.PlaceDto;
import com.dojangjjigae.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    /** Explore.jsx 가 쓰던 PLACES 배열을 그대로 대체 */
    @GetMapping
    public List<PlaceDto> listAll() {
        return placeService.listAll();
    }

    /** PlaceDetail.jsx 의 getPlace(id) 를 대체 (id 대신 slug 사용) */
    @GetMapping("/{slug}")
    public PlaceDto getBySlug(@PathVariable String slug) {
        return placeService.getBySlug(slug);
    }
}
