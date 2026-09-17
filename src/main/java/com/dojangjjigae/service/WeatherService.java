package com.dojangjjigae.service;

import com.dojangjjigae.dto.WeatherDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * OpenWeatherMap(무료 플랜)으로 천안 한 곳의 오늘 날씨를 가져온다.
 * 현재 날씨 + 3시간 예보(강수확률 · 최고기온) + 대기오염(미세먼지)을 합쳐 프론트 형식으로 보낸다.
 * 같은 결과를 일정 시간 캐시해서 새로고침마다 API를 부르지 않는다.
 * 키가 없거나 현재 날씨 호출이 실패하면 기본값(source=fallback)으로 대체해 화면이 깨지지 않게 한다.
 */
@Slf4j
@Service
public class WeatherService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.weather.api-key}")
    private String apiKey;

    @Value("${app.weather.lat}")
    private double lat;

    @Value("${app.weather.lon}")
    private double lon;

    @Value("${app.weather.cache-minutes:30}")
    private long cacheMinutes;

    private volatile WeatherDto cached;
    private volatile Instant cachedAt;

    public WeatherDto getTodayWeather() {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.isBlank()) {
            log.warn("[WeatherService] OPENWEATHER_API_KEY 가 없어 기본 날씨로 대체합니다.");
            return fallbackWeather();
        }

        WeatherDto hit = cached;
        if (hit != null && hit.getDate().equals(LocalDate.now(SEOUL))
                && Instant.now().isBefore(cachedAt.plus(Duration.ofMinutes(cacheMinutes)))) {
            return hit;
        }

        try {
            OpenWeatherResponse current = restClient.get()
                    .uri(BASE_URL + "/weather?lat={lat}&lon={lon}&appid={key}&units=metric&lang=kr", lat, lon, key)
                    .retrieve()
                    .body(OpenWeatherResponse.class);

            WeatherDto result = toDto(current, fetchForecast(key), fetchPm10(key));
            cached = result;
            cachedAt = Instant.now();
            log.info("[WeatherService] OpenWeather 호출 성공. temp={}, sky={}, pty={}, pop={}, pm10={}",
                    result.getTemp(), result.getSky(), result.getPty(), result.getPop(), result.getPm10());
            return result;
        } catch (Exception e) {
            log.error("[WeatherService] 날씨 API 호출 실패, 기본 날씨로 대체합니다.", e);
            return hit != null ? hit : fallbackWeather();
        }
    }

    /** 5일 · 3시간 예보. 실패하면 null (강수확률 0, 최고기온은 현재 값으로) */
    private JsonNode fetchForecast(String key) {
        try {
            String body = restClient.get()
                    .uri(BASE_URL + "/forecast?lat={lat}&lon={lon}&appid={key}&units=metric&cnt=8", lat, lon, key)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body).path("list");
        } catch (Exception e) {
            log.warn("[WeatherService] 예보 호출 실패 (강수확률 없이 판정): {}", e.getMessage());
            return null;
        }
    }

    /** 대기오염 API의 pm10. 실패하면 null */
    private Integer fetchPm10(String key) {
        try {
            String body = restClient.get()
                    .uri(BASE_URL + "/air_pollution?lat={lat}&lon={lon}&appid={key}", lat, lon, key)
                    .retrieve()
                    .body(String.class);
            JsonNode pm10 = objectMapper.readTree(body).path("list").path(0).path("components").path("pm10");
            return pm10.isNumber() ? (int) Math.round(pm10.asDouble()) : null;
        } catch (Exception e) {
            log.warn("[WeatherService] 대기오염 호출 실패 (미세먼지 없이 판정): {}", e.getMessage());
            return null;
        }
    }

    private WeatherDto toDto(OpenWeatherResponse res, JsonNode forecast, Integer pm10) {
        LocalDate today = LocalDate.now(SEOUL);
        boolean hasWeather = res.getWeather() != null && !res.getWeather().isEmpty();
        int weatherId = hasWeather ? res.getWeather().get(0).getId() : 800;
        String condition = hasWeather ? res.getWeather().get(0).getDescription() : "정보 없음";

        int pty = toPty(weatherId);
        double temp = res.getMain().getTemp();
        double tmx = res.getMain().getTempMax();
        int pop = 0;

        if (forecast != null && forecast.isArray() && forecast.size() > 0) {
            pop = (int) Math.round(forecast.get(0).path("pop").asDouble(0) * 100);
            for (JsonNode slot : forecast) {
                LocalDate slotDate = Instant.ofEpochSecond(slot.path("dt").asLong()).atZone(SEOUL).toLocalDate();
                if (slotDate.equals(today)) {
                    tmx = Math.max(tmx, slot.path("main").path("temp_max").asDouble(tmx));
                }
            }
        }

        return WeatherDto.builder()
                .date(today)
                .sky(toSky(weatherId, pty))
                .pty(pty)
                .temp(round1(temp))
                .tmx(round1(tmx))
                .pop(pop)
                .wind(res.getWind() == null ? 0 : round1(res.getWind().getSpeed()))
                .pm10(pm10)
                .condition(condition)
                .source("openweather")
                .build();
    }

    /** OpenWeather 날씨 코드 → 기상청 강수형태(PTY) */
    private int toPty(int id) {
        if (id >= 200 && id < 300) return 4;              // 뇌우 → 소나기
        if (id >= 300 && id < 400) return 5;              // 이슬비 → 빗방울
        if (id == 511) return 2;                          // 어는 비 → 비/눈
        if (id >= 520 && id < 600) return 4;              // 강한 소나기
        if (id >= 500 && id < 600) return 1;              // 비
        if (id >= 611 && id <= 616) return 2;             // 진눈깨비 → 비/눈
        if (id >= 600 && id < 700) return 3;              // 눈
        return 0;
    }

    /** OpenWeather 날씨 코드 → 기상청 하늘상태(SKY) */
    private int toSky(int id, int pty) {
        if (pty != 0 || (id >= 700 && id < 800)) return 4; // 강수 · 안개/연무 → 흐림
        if (id == 800) return 1;                           // 맑음
        if (id == 801 || id == 802) return 3;              // 구름 조금/튼구름 → 구름많음
        return 4;                                          // 흐림
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private WeatherDto fallbackWeather() {
        return WeatherDto.builder()
                .date(LocalDate.now(SEOUL))
                .sky(3)
                .pty(0)
                .temp(20)
                .tmx(null)
                .pop(0)
                .wind(0)
                .pm10(null)
                .condition("날씨 정보를 불러오지 못했어요")
                .source("fallback")
                .build();
    }
}
