package com.dojangjjigae.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** OpenWeatherMap "Current Weather Data" 응답의 필요한 부분만 매핑. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
class OpenWeatherResponse {
    private Main main;
    private List<Weather> weather;
    private Wind wind;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    static class Main {
        private double temp;

        @JsonProperty("temp_max")
        private double tempMax;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    static class Weather {
        private int id;
        private String description;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    static class Wind {
        private double speed;
    }
}
