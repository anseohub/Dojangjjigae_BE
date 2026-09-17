package com.dojangjjigae.config;

/**
 * 카카오 로그인을 붙이지 않고 테스트 계정으로 대체하기로 했으므로,
 * 모든 요청을 이 고정 유저 id로 처리한다.
 * 실제 로그인을 붙이게 되면 이 상수 대신 인증된 유저 id를 쓰도록 교체.
 */
public final class AppConstants {
    public static final Long TEST_USER_ID = 1L;

    private AppConstants() {}
}
