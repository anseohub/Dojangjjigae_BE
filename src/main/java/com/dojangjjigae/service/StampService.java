package com.dojangjjigae.service;

import com.dojangjjigae.config.AppConstants;
import com.dojangjjigae.config.GeoUtils;
import com.dojangjjigae.domain.*;
import com.dojangjjigae.dto.*;
import com.dojangjjigae.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 로그인을 붙이지 않기로 했으므로 모든 요청은 AppConstants.TEST_USER_ID 로 처리한다.
 * 실제 로그인을 붙이게 되면 이 상수 대신 인증된 유저 id를 쓰도록 교체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StampService {

    private static final double CERTIFY_RADIUS_M = 100.0;
    private static final DateTimeFormatter CERTIFIED_AT_FORMAT = DateTimeFormatter.ofPattern("MM.dd HH:mm");

    private final CourseRepository courseRepository;
    private final CourseSpotRepository courseSpotRepository;
    private final StampRecordRepository stampRecordRepository;
    private final UserCourseProgressRepository userCourseProgressRepository;
    private final TourApiClient tourApiClient;

    /**
     * 스탬프 찍기 전 반경 확인. 장소 좌표는 DB가 아니라 TourAPI 공통정보(contentId)에서 가져와
     * 프론트의 37개 장소 모두 같은 기준으로 판정한다. 판정은 서버에서만 한다.
     */
    public StampCheckResponse check(StampCheckRequest request) {
        Optional<TourApiClient.CommonDetail> common = tourApiClient.fetchCommon(request.getContentId());
        if (common.isEmpty() || common.get().lat() == null || common.get().lng() == null) {
            return StampCheckResponse.builder()
                    .withinRadius(false)
                    .distanceM(null)
                    .radiusM((int) CERTIFY_RADIUS_M)
                    .message("장소 좌표를 불러오지 못해 인증할 수 없어요. 잠시 후 다시 시도해 주세요.")
                    .build();
        }

        TourApiClient.CommonDetail place = common.get();
        double distance = GeoUtils.distanceMeters(request.getLat(), request.getLng(), place.lat(), place.lng());
        boolean within = distance <= CERTIFY_RADIUS_M;

        return StampCheckResponse.builder()
                .withinRadius(within)
                .distanceM(distance)
                .radiusM((int) CERTIFY_RADIUS_M)
                .message(within
                        ? "스탬프를 찍을 수 있어요."
                        : "장소 반경 " + (int) CERTIFY_RADIUS_M + "m 안에서만 스탬프를 찍을 수 있어요. (현재 " + Math.round(distance) + "m)")
                .build();
    }

    @Transactional
    public void startCourse(StartCourseRequest request) {
        Course course = findCourseBySlug(request.getCourseSlug());

        userCourseProgressRepository.save(UserCourseProgress.builder()
                .userId(AppConstants.TEST_USER_ID)
                .course(course)
                .startedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public CertifyResponse certify(CertifyRequest request) {
        Course course = findCourseBySlug(request.getCourseSlug());
        var courseSpots = courseSpotRepository.findByCourseIdOrderBySequenceAsc(course.getId());

        CourseSpot targetSpot = courseSpots.stream()
                .filter(cs -> cs.getPlace().getSlug().equals(request.getPlaceSlug()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("해당 코스에 속하지 않는 장소입니다."));

        Place place = targetSpot.getPlace();
        if (place.getLat() == null || place.getLng() == null) {
            throw new IllegalArgumentException("이 장소는 좌표 정보가 없어 GPS 인증이 불가능합니다.");
        }

        double distance = GeoUtils.distanceMeters(request.getLat(), request.getLng(), place.getLat(), place.getLng());
        int total = courseSpots.size();

        Optional<StampRecord> existing = stampRecordRepository.findByUserIdAndCourseIdAndPlaceId(
                AppConstants.TEST_USER_ID, course.getId(), place.getId());

        if (existing.isPresent() && existing.get().getCertifiedAt() != null) {
            return CertifyResponse.builder()
                    .certified(true)
                    .distanceM(distance)
                    .currentStampCount(currentCount(course.getId()))
                    .totalStampCount(total)
                    .message("이미 인증된 지점입니다.")
                    .build();
        }

        if (distance > CERTIFY_RADIUS_M) {
            return CertifyResponse.builder()
                    .certified(false)
                    .distanceM(distance)
                    .currentStampCount(currentCount(course.getId()))
                    .totalStampCount(total)
                    .message("현재 위치가 반경 밖이라 인증할 수 없습니다. (반경 " + (int) CERTIFY_RADIUS_M + "m)")
                    .build();
        }

        StampRecord record = existing.orElseGet(() -> StampRecord.builder()
                .userId(AppConstants.TEST_USER_ID)
                .course(course)
                .place(place)
                .build());
        record.setCertifiedAt(LocalDateTime.now());
        stampRecordRepository.save(record);

        return CertifyResponse.builder()
                .certified(true)
                .distanceM(distance)
                .currentStampCount(currentCount(course.getId()))
                .totalStampCount(total)
                .message("스탬프를 획득했어요!")
                .build();
    }

    public CourseProgressDto getCurrentProgress(Double lat, Double lng) {
        UserCourseProgress progress = userCourseProgressRepository
                .findFirstByUserIdOrderByStartedAtDesc(AppConstants.TEST_USER_ID)
                .orElseThrow(() -> new NoSuchElementException("진행중인 코스가 없습니다."));

        Course course = progress.getCourse();
        var courseSpots = courseSpotRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        var stampRecords = stampRecordRepository.findByUserIdAndCourseId(AppConstants.TEST_USER_ID, course.getId());

        int total = courseSpots.size();
        int current = (int) stampRecords.stream().filter(r -> r.getCertifiedAt() != null).count();

        Double nextSpotDistance = null;
        if (lat != null && lng != null) {
            nextSpotDistance = courseSpots.stream()
                    .filter(cs -> stampRecords.stream().noneMatch(r ->
                            r.getPlace().getId().equals(cs.getPlace().getId()) && r.getCertifiedAt() != null))
                    .filter(cs -> cs.getPlace().getLat() != null && cs.getPlace().getLng() != null)
                    .map(cs -> GeoUtils.distanceMeters(lat, lng, cs.getPlace().getLat(), cs.getPlace().getLng()))
                    .min(Double::compareTo)
                    .orElse(null);
        }

        return CourseProgressDto.builder()
                .courseId(course.getSlug())
                .title(course.getTitle())
                .chips(List.of("진행중", course.getType()))
                .currentStampCount(current)
                .totalStampCount(total)
                .nextSpotDistanceM(nextSpotDistance)
                .startDateLabel(progress.getStartedAt().format(DateTimeFormatter.ofPattern("MM.dd")))
                .totalDistanceKm(null)
                .estimatedMinutes(null)
                .build();
    }

    public List<SpotStatusDto> getSpotsStatus(String courseSlug, Double lat, Double lng) {
        Course course = findCourseBySlug(courseSlug);
        var courseSpots = courseSpotRepository.findByCourseIdOrderBySequenceAsc(course.getId());
        var stampRecords = stampRecordRepository.findByUserIdAndCourseId(AppConstants.TEST_USER_ID, course.getId());

        return courseSpots.stream().map(cs -> {
            Place place = cs.getPlace();

            Optional<StampRecord> record = stampRecords.stream()
                    .filter(r -> r.getPlace().getId().equals(place.getId()) && r.getCertifiedAt() != null)
                    .findFirst();

            SpotStatusDto.Status status;
            Double distance = null;

            if (record.isPresent()) {
                status = SpotStatusDto.Status.CERTIFIED;
            } else if (lat != null && lng != null && place.getLat() != null && place.getLng() != null) {
                distance = GeoUtils.distanceMeters(lat, lng, place.getLat(), place.getLng());
                status = distance <= CERTIFY_RADIUS_M
                        ? SpotStatusDto.Status.AVAILABLE
                        : SpotStatusDto.Status.OUT_OF_RANGE;
            } else {
                status = SpotStatusDto.Status.OUT_OF_RANGE;
            }

            return SpotStatusDto.builder()
                    .sequence(cs.getSequence())
                    .placeId(place.getSlug())
                    .name(place.getName())
                    .imageUrl(place.getPhotos().isEmpty() ? null : place.getPhotos().get(0).getUrl())
                    .tags(place.getPetRules())
                    .status(status)
                    .certifiedAtLabel(record.map(r -> r.getCertifiedAt().format(CERTIFIED_AT_FORMAT) + " 인증").orElse(null))
                    .distanceM(distance)
                    .build();
        }).toList();
    }

    private Course findCourseBySlug(String slug) {
        return courseRepository.findBySlug(slug)
                .orElseThrow(() -> new NoSuchElementException("코스를 찾을 수 없습니다. slug=" + slug));
    }

    private int currentCount(Long courseId) {
        return (int) stampRecordRepository.countByUserIdAndCourseIdAndCertifiedAtIsNotNull(
                AppConstants.TEST_USER_ID, courseId);
    }
}
