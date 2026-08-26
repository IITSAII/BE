package com.iitsaii.photobooth.session.entity;

/** 오늘의 관계 (2단계 선택). null이면 "설정 안 함"으로 취급한다. */
public enum RelationshipType {
    GETTING_CLOSE,
    FRIEND,
    CRUSH,
    COUPLE
}
