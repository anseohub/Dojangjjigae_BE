package com.dojangjjigae.config;

import com.dojangjjigae.domain.*;
import com.dojangjjigae.repository.CourseRepository;
import com.dojangjjigae.repository.CourseSpotRepository;
import com.dojangjjigae.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 프론트 data/places.js, data/courses.js 에 있는 실제 큐레이션 데이터를 그대로 옮겨 심는다.
 * 두 파일과 내용이 달라지면 이 시더도 같이 업데이트해야 한다 (지금은 수동 동기화).
 *
 * 이미 데이터가 있으면 건너뛰므로 재실행해도 중복 적재되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@Order(1)
public class CuratedDataSeeder implements CommandLineRunner {

    private final PlaceRepository placeRepository;
    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;

    @Override
    public void run(String... args) {
        if (placeRepository.count() > 0) {
            log.info("[CuratedDataSeeder] 이미 장소 데이터가 있어 시드를 건너뜁니다.");
            return;
        }

        Map<String, Place> places = new LinkedHashMap<>();
        for (Place place : buildPlaces()) {
            places.put(place.getSlug(), placeRepository.save(place));
        }
        log.info("[CuratedDataSeeder] 장소 {}건 적재 완료.", places.size());

        int courseCount = 0;
        for (CourseSeed seed : buildCourses()) {
            Course course = courseRepository.save(Course.builder()
                    .slug(seed.slug)
                    .title(seed.title)
                    .type(seed.type)
                    .distance("")
                    .duration("")
                    .desc(List.of("", ""))
                    .build());

            int sequence = 1;
            for (String placeSlug : seed.placeIds) {
                Place place = places.get(placeSlug);
                if (place == null) {
                    log.warn("[CuratedDataSeeder] 코스 '{}' 에서 장소 slug '{}' 를 찾을 수 없습니다.", seed.slug, placeSlug);
                    continue;
                }
                courseSpotRepository.save(CourseSpot.builder()
                        .course(course)
                        .place(place)
                        .sequence(sequence++)
                        .build());
            }
            courseCount++;
        }
        log.info("[CuratedDataSeeder] 코스 {}건 적재 완료.", courseCount);
    }

    // ---------- 장소 13개 ----------

    private List<Place> buildPlaces() {
        return List.of(
                place("independence-hall", "독립기념관", "문화 · 역사", false,
                        "충남 천안시 동남구 목천읍 독립기념관로 1", null, null, "", // TODO: 독립기념관 contentId 직접 확인 후 채우기
                        PetPolicy.ASSISTANCE_DOG_ONLY, null, true, 2,
                        List.of("반려동물 입장 불가", "장애인 보조견 동반 가능", "태극기나무 포토스팟"),
                        photo("2506024", "https://tong.visitkorea.or.kr/cms2/website/24/2506024.jpg", "IR 스튜디오", "201706"),
                        photo("2506026", "https://tong.visitkorea.or.kr/cms2/website/26/2506026.jpg", "IR 스튜디오", "201706"),
                        photo("2506034", "https://tong.visitkorea.or.kr/cms2/website/34/2506034.jpg", "IR 스튜디오", "201706"),
                        photo("3391169", "https://tong.visitkorea.or.kr/cms2/website/69/3391169.jpg", "노희완", "202408"),
                        photo("3473217", "https://tong.visitkorea.or.kr/cms2/website/17/3473217.jpg", "김영호", "202411"),
                        photo("1960271", "https://tong.visitkorea.or.kr/cms2/website/71/1960271.jpg", "한승헌", "201400")
                ),
                place("gakwonsa", "각원사", "문화 · 역사", false,
                        "충남 천안시 동남구 각원사길 245", null, null, "125885",
                        PetPolicy.ALLOWED, null, true, 1,
                        List.of("반려동물 동반 가능 사찰"),
                        photo("2600765", "https://tong.visitkorea.or.kr/cms2/website/65/2600765.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600768", "https://tong.visitkorea.or.kr/cms2/website/68/2600768.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600775", "https://tong.visitkorea.or.kr/cms2/website/75/2600775.jpg", "한국관광공사 김지호", "201902"),
                        photo("3403656", "https://tong.visitkorea.or.kr/cms2/website/56/3403656.jpg", "김영호", "202409"),
                        photo("3403700", "https://tong.visitkorea.or.kr/cms2/website/00/3403700.jpg", "김영호", "202409")
                ),
                place("seongseong-lake", "성성호수공원", "공원 · 산책로", false,
                        "충남 천안시 서북구 성성2길 66", null, null, "2824534",
                        PetPolicy.CONDITIONAL, null, true, 1,
                        List.of("반려동물 산책 가능", "어린이 놀이터 출입 불가"),
                        photo("3403735", "https://tong.visitkorea.or.kr/cms2/website/35/3403735.jpg", "김영호", "202409"),
                        photo("3403738", "https://tong.visitkorea.or.kr/cms2/website/38/3403738.jpg", "김영호", "202409"),
                        photo("3403782", "https://tong.visitkorea.or.kr/cms2/website/82/3403782.jpg", "김영호", "202409"),
                        photo("3403820", "https://tong.visitkorea.or.kr/cms2/website/20/3403820.jpg", "김영호", "202409"),
                        photo("3403859", "https://tong.visitkorea.or.kr/cms2/website/59/3403859.jpg", "김영호", "202409")
                ),
                place("gwangdeoksa", "천안 광덕사", "문화 · 역사", false,
                        "충남 천안시 동남구 광덕면 광덕사길 26", null, null, "127233",
                        PetPolicy.CONDITIONAL, null, true, 1,
                        List.of("건물 내 출입 불가", "인근 계곡 · 등산로 동반 가능"),
                        photo("2600839", "https://tong.visitkorea.or.kr/cms2/website/39/2600839.jpg", "한국관광공사 김지호", "201902"),
                        photo("3403924", "https://tong.visitkorea.or.kr/cms2/website/24/3403924.jpg", "한국관광공사 김지호", "202409"),
                        photo("3404305", "https://tong.visitkorea.or.kr/cms2/website/05/3404305.jpg", "한국관광공사 김지호", "202409"),
                        photo("3404262", "https://tong.visitkorea.or.kr/cms2/website/62/3404262.jpg", "한국관광공사 김지호", "202409")
                ),
                place("yugwansun", "천안 유관순 열사 유적", "문화 · 역사", false,
                        "충남 천안시 동남구 병천면 유관순길 38", null, null, "125926",
                        PetPolicy.ASSISTANCE_DOG_ONLY, null, true, 1,
                        List.of("반려동물 입장 불가", "장애인 보조견 동반 가능"),
                        photo("2600867", "https://tong.visitkorea.or.kr/cms2/website/67/2600867.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600870", "https://tong.visitkorea.or.kr/cms2/website/70/2600870.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600879", "https://tong.visitkorea.or.kr/cms2/website/79/2600879.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600882", "https://tong.visitkorea.or.kr/cms2/website/82/2600882.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600889", "https://tong.visitkorea.or.kr/cms2/website/89/2600889.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600893", "https://tong.visitkorea.or.kr/cms2/website/93/2600893.jpg", "한국관광공사 김지호", "201902")
                ),
                place("gwangdeoksan", "광덕산", "공원 · 산책로", false,
                        "충남 천안시 동남구 광덕면", null, null, "127200",
                        PetPolicy.UNKNOWN, null, true, 2,
                        List.of("등산 후 계곡 물놀이 · 바베큐 가능"),
                        photo("3404350", "https://tong.visitkorea.or.kr/cms2/website/50/3404350.jpg", "김영호", "202409"),
                        photo("3404354", "https://tong.visitkorea.or.kr/cms2/website/54/3404354.jpg", "김영호", "202409"),
                        photo("3404358", "https://tong.visitkorea.or.kr/cms2/website/58/3404358.jpg", "김영호", "202409"),
                        photo("3404332", "https://tong.visitkorea.or.kr/cms2/website/32/3404332.jpg", "김영호", "202409")
                ),
                place("bongseon-honggyeongsa", "봉선홍경사 갈기비", "문화 · 역사", false,
                        "충남 천안시 서북구 성환읍 대홍3길 77-48", null, null, "125939",
                        PetPolicy.ALLOWED, null, true, 1,
                        List.of("반려동물 동반 가능(야외)"),
                        photo("3404370", "https://tong.visitkorea.or.kr/cms2/website/70/3404370.jpg", "김영호", "202409"),
                        photo("3404383", "https://tong.visitkorea.or.kr/cms2/website/83/3404383.jpg", "김영호", "202409"),
                        photo("3404398", "https://tong.visitkorea.or.kr/cms2/website/98/3404398.jpg", "김영호", "202409"),
                        photo("3404400", "https://tong.visitkorea.or.kr/cms2/website/00/3404400.jpg", "김영호", "202409")
                ),
                place("pins-coffee", "천안카페 (핀스커피)", "카페 · 식당", false,
                        "충남 천안시 동남구 해솔1길 27-29", null, null, "2839108",
                        PetPolicy.CONDITIONAL, null, true, 1,
                        List.of("반려동물 동반 가능", "테라스 · 야외 좌석 이용"),
                        photo("3473258", "https://tong.visitkorea.or.kr/cms2/website/58/3473258.jpg", "김영호", "202411"),
                        photo("3473259", "https://tong.visitkorea.or.kr/cms2/website/59/3473259.jpg", "김영호", "202411"),
                        photo("3473264", "https://tong.visitkorea.or.kr/cms2/website/64/3473264.jpg", "김영호", "202411"),
                        photo("3473265", "https://tong.visitkorea.or.kr/cms2/website/65/3473265.jpg", "김영호", "202411")
                ),
                place("aunae-park", "아우내독립만세운동 기념공원", "문화 · 역사", false,
                        "충청남도 천안시 동남구 병천면 아우내장터1길 12-23", null, null, "2778533",
                        PetPolicy.ALLOWED, null, true, 1,
                        List.of("반려동물 동반 가능"),
                        photo("2600797", "https://tong.visitkorea.or.kr/cms2/website/97/2600797.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600800", "https://tong.visitkorea.or.kr/cms2/website/00/2600800.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600802", "https://tong.visitkorea.or.kr/cms2/website/02/2600802.jpg", "한국관광공사 김지호", "201902"),
                        photo("2600804", "https://tong.visitkorea.or.kr/cms2/website/04/2600804.jpg", "한국관광공사 김지호", "201902")
                ),
                place("shinsegae", "신세계백화점 천안아산점", "실내", true,
                        "충남 천안시 동남구 만남로 43 신세계 천안아산점", 36.8193302097389, 127.15784651678, "2923877",
                        PetPolicy.CONDITIONAL, 10, true, 2,
                        List.of("10kg 이하 소형견만 이용 가능", "뚜껑이 덮인 케이지나 유모차 이용 필수",
                                "식품관 · 아동관 · 식당가 입장 불가", "브랜드 정책에 따라 매장 입장이 불가할 수 있음")
                ),
                place("wonderplace", "원더플레이스 천안신부점", "실내", true,
                        "충남 천안시 동남구 먹거리10길 27", 36.8179783801037, 127.156882559012, "4026895",
                        PetPolicy.ALLOWED, null, true, 1,
                        List.of("펫 가능", "천안 애견동반 쇼핑몰")
                ),
                place("galleria", "갤러리아백화점 센터시티점", "실내", true,
                        "충남 천안시 서북구 공원로 227 갤러리아 센터시티", 36.800564020263, 127.104726252319, "4026897",
                        PetPolicy.CONDITIONAL, null, true, 2,
                        List.of("안내견 모든 시설 출입 가능", "식품관 · 식당가 출입 불가",
                                "일반매장은 케이지 · 펫모차 이용 시 출입 가능 (슬링백 이용 불가)", "맹견은 고객 안전을 위해 출입 제한")
                ),
                place("moda-outlet", "모다아울렛 천안아산점", "실내", true,
                        "충청남도 천안시 서북구 공원로 196 (불당동, 펜타포트)", 36.7984279064312, 127.101429867826, "3306315",
                        PetPolicy.CONDITIONAL, null, true, 2,
                        List.of("강아지 유모차 무료 대여", "식당 출입 불가", "케이지나 유모차만 가능")
                )
        );
    }

    // ---------- 코스 6개 ----------

    private List<CourseSeed> buildCourses() {
        return List.of(
                new CourseSeed("gwangdeok-nature", "광덕사에서 광덕산까지 자연 코스", "야외",
                        List.of("gwangdeoksa", "gwangdeoksan")),
                new CourseSeed("byeongcheon-history", "독립기념관 · 병천 역사 코스", "야외",
                        List.of("independence-hall", "yugwansun", "aunae-park")),
                new CourseSeed("seobuk-walk", "성성호수공원 · 홍경사 산책 코스", "야외",
                        List.of("seongseong-lake", "bongseon-honggyeongsa")),
                new CourseSeed("gakwonsa-cafe", "각원사 · 카페 코스", "야외 · 실내",
                        List.of("gakwonsa", "pins-coffee")),
                new CourseSeed("sinbu-indoor", "비 올 때 신부동 실내 코스", "실내",
                        List.of("shinsegae", "wonderplace")),
                new CourseSeed("buldang-indoor", "비 올 때 불당동 실내 코스", "실내",
                        List.of("galleria", "moda-outlet"))
        );
    }

    // ---------- 헬퍼 ----------

    private record CourseSeed(String slug, String title, String type, List<String> placeIds) {}

    private Place place(String slug, String name, String category, boolean indoor,
                         String address, Double lat, Double lng, String contentId,
                         PetPolicy policy, Integer maxWeightKg, boolean assistanceDogAllowed,
                         int stampCount, List<String> petRules, PlacePhoto... photos) {
        return Place.builder()
                .slug(slug)
                .name(name)
                .category(category)
                .indoor(indoor)
                .address(address)
                .lat(lat)
                .lng(lng)
                .contentId(contentId)
                .policy(policy)
                .maxWeightKg(maxWeightKg)
                .assistanceDogAllowed(assistanceDogAllowed)
                .stampCount(stampCount)
                .petRules(petRules)
                .summary("")
                .description("")
                .info(PlaceInfo.builder().hours("").closed("").parking("").tel("").fee("").build())
                .photos(List.of(photos))
                .build();
    }

    private PlacePhoto photo(String id, String url, String photographer, String month) {
        return PlacePhoto.builder().photoId(id).url(url).photographer(photographer).month(month).build();
    }
}
