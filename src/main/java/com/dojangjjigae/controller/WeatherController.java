package com.dojangjjigae.controller;

import com.dojangjjigae.dto.WeatherDto;
import com.dojangjjigae.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/today")
    public WeatherDto today() {
        return weatherService.getTodayWeather();
    }
}
