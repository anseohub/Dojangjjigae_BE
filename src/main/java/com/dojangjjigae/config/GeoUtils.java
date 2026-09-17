package com.dojangjjigae.config;

/**
 * 두 GPS 좌표 사이의 거리를 미터 단위로 계산한다.
 * 스탬프 인증 판정에 사용 — 반드시 서버에서 계산해야 클라이언트 좌표 위변조를 막을 수 있다.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000;

    private GeoUtils() {}

    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_M * c;
    }
}
