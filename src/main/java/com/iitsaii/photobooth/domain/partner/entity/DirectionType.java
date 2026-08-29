package com.iitsaii.photobooth.domain.partner.entity;

/** 제휴 업체 길찾기 정보 제공 방식. */
public enum DirectionType {

    /** direction_url이 네이버 지도 URL. 프론트에서 새 탭으로 이동 */
    MAP_URL,

    /** direction_url이 도보 경로 GIF의 S3 URL. 프론트에서 그 자리에서 GIF 재생 */
    WALK_GIF
}
