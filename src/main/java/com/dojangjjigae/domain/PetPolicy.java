package com.dojangjjigae.domain;

/** 프론트 data/policy.js 의 PET_POLICY 값과 문자열이 정확히 일치해야 한다. */
public enum PetPolicy {
    ALLOWED,
    CONDITIONAL,
    ASSISTANCE_DOG_ONLY,
    UNKNOWN
}
