package com.dojangjjigae.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국관광공사 TourAPI(KorService2)의 detailCommon2 를 실시간으로 호출한다.
 *
 * 장소 상세 조회(PlaceService.getBySlug) 시마다 호출되므로, 서비스가 운영되는 동안
 * 실제 API 호출 기록이 데이터포털 계정에 쌓인다 — 공모전 "서비스 내 OpenAPI 활용내역"
 * 확인 항목에 대응하기 위한 부분.
 *
 * contentId 가 있는 장소(신세계/원더플레이스/갤러리아/모다아울렛)에서만 동작하고,
 * 없는 장소는 조용히 건너뛴다. 호출이 실패해도 기존 큐레이션 데이터는 그대로 보여준다.
 */
@Slf4j
@Service
public class TourApiClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorService2/detailCommon2";
    private static final String IMAGE_URL = "https://apis.data.go.kr/B551011/KorService2/detailImage2";
    private static final String GALLERY_URL = "https://apis.data.go.kr/B551011/PhotoGalleryService1/gallerySearchList1";
    private static final String PET_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.tourapi.service-key}")
    private String serviceKey;

    // 캐시 유지 시간(분). 0이면 캐시를 쓰지 않고 매번 호출한다.
    @Value("${app.tourapi.cache-minutes:360}")
    private long cacheMinutes;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(Object value, Instant expiresAt) {}

    @SuppressWarnings("unchecked")
    private <T> T getCached(String cacheKey) {
        if (cacheMinutes <= 0) return null;
        CacheEntry entry = cache.get(cacheKey);
        if (entry == null) return null;
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(cacheKey);
            return null;
        }
        log.info("[TourApiClient] 캐시 사용: {}", cacheKey);
        return (T) entry.value();
    }

    private void putCache(String cacheKey, Object value) {
        if (cacheMinutes <= 0) return;
        cache.put(cacheKey, new CacheEntry(value, Instant.now().plus(Duration.ofMinutes(cacheMinutes))));
    }

    public record TourApiDetail(String overview, String tel) {}

    public Optional<TourApiDetail> fetchDetail(String contentId) {
        String key = serviceKey == null ? "" : serviceKey.trim();

        if (key.isBlank() || contentId == null || contentId.isBlank()) {
            log.warn("[TourApiClient] 서비스키가 비어있어 호출을 건너뜁니다.");
            return Optional.empty();
        }

        // 디버그용: 키 앞 10자리 + 길이만 로그로 남긴다 (전체 키는 노출 안 함)
        log.info("[TourApiClient] 사용중인 키 미리보기: {}... (총 길이 {})",
                key.substring(0, Math.min(10, key.length())), key.length());

        try {
            String url = BASE_URL
                    + "?serviceKey=" + key
                    + "&contentId=" + contentId
                    + "&numOfRows=1&pageNo=1"
                    + "&MobileOS=WEB&MobileApp=dojangjjigae&_type=json";

            String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            log.info("[TourApiClient] 원본 응답: {}", body); // 디버그용 - 원인 파악되면 지울 것
            JsonNode root = objectMapper.readTree(body);

            JsonNode header = root.path("response").path("header");
            if (!"0000".equals(header.path("resultCode").asText())) {
                log.warn("[TourApiClient] resultCode={} contentId={}", header.path("resultCode").asText(), contentId);
                return Optional.empty();
            }

            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");
            JsonNode item = itemsNode.isArray() ? itemsNode.get(0) : itemsNode;
            if (item == null || item.isMissingNode()) return Optional.empty();

            String overview = item.path("overview").asText("");
            String tel = item.path("tel").asText("");

            log.info("[TourApiClient] TourAPI 실시간 호출 성공 (실제 데이터포털 호출 기록에 남음). contentId={}", contentId);
            return Optional.of(new TourApiDetail(overview, tel));
        } catch (Exception e) {
            log.error("[TourApiClient] TourAPI 호출 실패. contentId={}", contentId, e);
            return Optional.empty();
        }
    }

    /**
     * detailImage2 를 실시간으로 호출해 contentId 에 해당하는 장소 사진 목록을 가져온다.
     * 큐레이션 사진이 이미 있는 장소는 PlaceService 쪽에서 이 메서드를 호출하지 않는다.
     */
    public List<String> fetchPhotos(String contentId) {
        String key = serviceKey == null ? "" : serviceKey.trim();

        // 1. 키나 contentId 없으면 호출 스킵
        if (key.isBlank() || contentId == null || contentId.isBlank()) {
            return List.of();
        }

        String cacheKey = "image:" + contentId;
        List<String> cached = getCached(cacheKey);
        if (cached != null) return cached;

        try {
            // 2. detailCommon2와 동일한 패턴으로 URL 조립
            String url = IMAGE_URL
                    + "?serviceKey=" + key
                    + "&contentId=" + contentId
                    + "&imageYN=Y&numOfRows=10&pageNo=1"
                    + "&MobileOS=WEB&MobileApp=dojangjjigae&_type=json";

            String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            // 3. 응답 코드 체크
            JsonNode header = root.path("response").path("header");
            if (!"0000".equals(header.path("resultCode").asText())) {
                log.warn("[TourApiClient] detailImage2 resultCode={} contentId={}", header.path("resultCode").asText(), contentId);
                return List.of();
            }

            // 4. item이 배열일 수도, 단일 객체일 수도 있음
            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            List<String> urls = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) urls.add(item.path("originimgurl").asText("").replace("http://", "https://"));
            } else if (!itemsNode.isMissingNode()) {
                urls.add(itemsNode.path("originimgurl").asText("").replace("http://", "https://"));
            }

            // 추가 사진이 없는 경우도 성공 결과로 저장해 반복 호출을 막는다
            List<String> result = urls.stream().filter(u -> !u.isBlank()).toList();
            putCache(cacheKey, result);
            return result;
        } catch (Exception e) {
            log.error("[TourApiClient] detailImage2 호출 실패. contentId={}", contentId, e);
            return List.of();
        }
    }

    /**
     * detailCommon2 를 실시간으로 호출해 주소·좌표·개요·대표이미지를 가져온다.
     * PublicDataController(/api/public/places/{contentId})가 사용하는, 실제로 화면에 쓰이는 경로다.
     */
    public record CommonDetail(String address, Double lat, Double lng, String overview, List<String> images, String contentTypeId) {}

    public Optional<CommonDetail> fetchCommon(String contentId) {
        String key = serviceKey == null ? "" : serviceKey.trim();
        if (key.isBlank() || contentId == null || contentId.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = "common:" + contentId;
        CommonDetail cached = getCached(cacheKey);
        if (cached != null) return Optional.of(cached);

        try {
            String url = BASE_URL
                    + "?serviceKey=" + key
                    + "&contentId=" + contentId
                    + "&numOfRows=1&pageNo=1"
                    + "&MobileOS=WEB&MobileApp=dojangjjigae&_type=json";

            String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            JsonNode header = root.path("response").path("header");
            if (!"0000".equals(header.path("resultCode").asText())) {
                log.warn("[TourApiClient] detailCommon2(공통) resultCode={} contentId={}", header.path("resultCode").asText(), contentId);
                return Optional.empty();
            }

            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");
            JsonNode item = itemsNode.isArray() ? itemsNode.get(0) : itemsNode;
            if (item == null || item.isMissingNode()) return Optional.empty();

            String addr1 = item.path("addr1").asText("");
            String addr2 = item.path("addr2").asText("");
            String address = (addr1 + " " + addr2).trim();
            Double lat = parseCoord(item.path("mapy").asText(""));
            Double lng = parseCoord(item.path("mapx").asText(""));
            String overview = item.path("overview").asText("");

            List<String> images = new ArrayList<>();
            String firstimage = item.path("firstimage").asText("").replace("http://", "https://");
            String firstimage2 = item.path("firstimage2").asText("").replace("http://", "https://");
            if (!firstimage.isBlank()) images.add(firstimage);
            if (!firstimage2.isBlank() && !firstimage2.equals(firstimage)) images.add(firstimage2);

            CommonDetail detail = new CommonDetail(address, lat, lng, overview, images, item.path("contenttypeid").asText(""));
            putCache(cacheKey, detail);
            log.info("[TourApiClient] detailCommon2 실시간 호출 성공. contentId={}", contentId);
            return Optional.of(detail);
        } catch (Exception e) {
            log.error("[TourApiClient] detailCommon2(공통) 호출 실패. contentId={}", contentId, e);
            return Optional.empty();
        }
    }

    private Double parseCoord(String value) {
        try {
            return (value == null || value.isBlank()) ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * PhotoGalleryService1/gallerySearchList1 을 키워드로 실시간 호출한다.
     * '관광사진' 갤러리(장소 이름과 무관하게 독립적으로 등록된 사진 DB)에서 검색하는 것이라
     * contentId가 아니라 키워드(장소명)로 찾는다.
     */
    public record GalleryPhoto(String contentId, String url, String photographer, String month) {}

    public List<GalleryPhoto> fetchGalleryByKeyword(String keyword) {
        String key = serviceKey == null ? "" : serviceKey.trim();
        if (key.isBlank() || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String cacheKey = "gallery:" + keyword;
        List<GalleryPhoto> cached = getCached(cacheKey);
        if (cached != null) return cached;

        try {
            String url = GALLERY_URL
                    + "?serviceKey=" + key
                    + "&keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8).replace("+", "%20")
                    + "&numOfRows=100&pageNo=1"
                    + "&MobileOS=ETC&MobileApp=dojangjjigae&_type=json";

            String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            JsonNode header = root.path("response").path("header");
            if (!"0000".equals(header.path("resultCode").asText())) {
                log.warn("[TourApiClient] gallerySearchList1 resultCode={} keyword={}", header.path("resultCode").asText(), keyword);
                return List.of();
            }

            JsonNode itemsNode = root.path("response").path("body").path("items").path("item");

            List<GalleryPhoto> photos = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) photos.add(toGalleryPhoto(item));
            } else if (!itemsNode.isMissingNode()) {
                photos.add(toGalleryPhoto(itemsNode));
            }

            List<GalleryPhoto> result = List.copyOf(photos);
            putCache(cacheKey, result);
            log.info("[TourApiClient] gallerySearchList1 실시간 호출 성공. keyword={}, 결과={}건", keyword, result.size());
            return result;
        } catch (Exception e) {
            log.error("[TourApiClient] gallerySearchList1 호출 실패. keyword={}", keyword, e);
            return List.of();
        }
    }

    /**
     * 반려동물 동반여행 서비스(KorPetTourService2) detailPetTour2 를 호출해 동반 조건 문장 목록을 만든다.
     * 값이 비어 있는 항목은 건너뛴다.
     */
    public Optional<List<String>> fetchPetTour(String contentId) {
        String key = serviceKey == null ? "" : serviceKey.trim();
        if (key.isBlank() || contentId == null || contentId.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = "pet:" + contentId;
        List<String> cached = getCached(cacheKey);
        if (cached != null) return Optional.of(cached);

        try {
            String url = PET_URL + "/detailPetTour2"
                    + "?serviceKey=" + key
                    + "&contentId=" + contentId
                    + "&numOfRows=1&pageNo=1"
                    + "&MobileOS=ETC&MobileApp=dojangjjigae&_type=json";

            JsonNode item = requestFirstItem(url, "detailPetTour2", contentId);
            if (item == null) return Optional.empty();

            List<String> rules = new ArrayList<>();
            addLines(rules, "", item.path("acmpyTypeCd").asText(""));
            addLines(rules, "동반 가능 동물 : ", item.path("acmpyPsblCpam").asText(""));
            addLines(rules, "동반 시 필요사항 : ", item.path("acmpyNeedMtr").asText(""));
            addLines(rules, "", item.path("etcAcmpyInfo").asText(""));
            addLines(rules, "구비 시설 : ", item.path("relaPosesFclty").asText(""));
            addLines(rules, "비치 품목 : ", item.path("relaFrnshPrdlst").asText(""));
            addLines(rules, "대여 품목 : ", item.path("relaRntlPrdlst").asText(""));
            addLines(rules, "구매 품목 : ", item.path("relaPurcPrdlst").asText(""));
            addLines(rules, "사고 대비사항 : ", item.path("relaAcdntRiskMtr").asText(""));

            List<String> result = List.copyOf(rules);
            putCache(cacheKey, result);
            log.info("[TourApiClient] detailPetTour2 실시간 호출 성공. contentId={}, 항목={}건", contentId, result.size());
            return Optional.of(result);
        } catch (Exception e) {
            log.error("[TourApiClient] detailPetTour2 호출 실패. contentId={}", contentId, e);
            return Optional.empty();
        }
    }

    /** 이용 정보(운영시간 · 휴무일 · 주차 · 입장료). 관광타입마다 필드명이 달라 후보 필드 중 값이 있는 것을 쓴다. */
    public record PetIntro(List<String> hours, List<String> closed, List<String> parking, List<String> fee) {}

    public Optional<PetIntro> fetchPetIntro(String contentId, String contentTypeId) {
        String key = serviceKey == null ? "" : serviceKey.trim();
        if (key.isBlank() || contentId == null || contentId.isBlank()
                || contentTypeId == null || contentTypeId.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = "petintro:" + contentId;
        PetIntro cached = getCached(cacheKey);
        if (cached != null) return Optional.of(cached);

        try {
            String url = PET_URL + "/detailIntro2"
                    + "?serviceKey=" + key
                    + "&contentId=" + contentId
                    + "&contentTypeId=" + contentTypeId
                    + "&numOfRows=1&pageNo=1"
                    + "&MobileOS=ETC&MobileApp=dojangjjigae&_type=json";

            JsonNode item = requestFirstItem(url, "detailIntro2", contentId);
            if (item == null) return Optional.empty();

            PetIntro intro = new PetIntro(
                    firstLines(item, "opentime", "usetime", "usetimeculture", "usetimeleports", "opentimefood", "playtime"),
                    firstLines(item, "restdate", "restdateshopping", "restdateculture", "restdateleports", "restdatefood"),
                    firstLines(item, "parking", "parkingshopping", "parkingculture", "parkingleports", "parkingfood", "parkinglodging"),
                    firstLines(item, "usefee", "usefeeleports")
            );
            putCache(cacheKey, intro);
            log.info("[TourApiClient] detailIntro2(반려동물) 실시간 호출 성공. contentId={}", contentId);
            return Optional.of(intro);
        } catch (Exception e) {
            log.error("[TourApiClient] detailIntro2(반려동물) 호출 실패. contentId={}", contentId, e);
            return Optional.empty();
        }
    }

    /** 결과코드가 0000이 아니면 null, 0000인데 결과가 없으면 MissingNode를 돌려준다 */
    private JsonNode requestFirstItem(String url, String operation, String contentId) throws Exception {
        String body = restClient.get().uri(URI.create(url)).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(body);

        JsonNode header = root.path("response").path("header");
        if (!"0000".equals(header.path("resultCode").asText())) {
            log.warn("[TourApiClient] {} resultCode={} contentId={}", operation, header.path("resultCode").asText(), contentId);
            return null;
        }

        JsonNode itemsNode = root.path("response").path("body").path("items").path("item");
        if (itemsNode.isArray()) {
            return itemsNode.size() > 0 ? itemsNode.get(0) : itemsNode.path("none");
        }
        return itemsNode;
    }

    private List<String> firstLines(JsonNode item, String... fields) {
        for (String field : fields) {
            List<String> lines = new ArrayList<>();
            addLines(lines, "", item.path(field).asText(""));
            if (!lines.isEmpty()) return lines;
        }
        return List.of();
    }

    /** <br> 태그와 줄바꿈으로 나누고, HTML 태그와 앞의 "- " 기호를 지워 한 줄씩 담는다 */
    private void addLines(List<String> target, String prefix, String raw) {
        if (raw == null || raw.isBlank()) return;
        String text = raw.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "");
        for (String line : text.split("\n")) {
            String cleaned = line.trim().replaceFirst("^[-•·]\\s*", "").trim();
            if (!cleaned.isBlank()) target.add(prefix + cleaned);
        }
    }

    private GalleryPhoto toGalleryPhoto(JsonNode item) {
        return new GalleryPhoto(
                item.path("galContentId").asText(""),
                item.path("galWebImageUrl").asText(""),
                item.path("galPhotographer").asText(""),
                item.path("galPhotographyMonth").asText("")
        );
    }

}
