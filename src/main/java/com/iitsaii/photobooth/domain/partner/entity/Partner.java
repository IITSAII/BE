package com.iitsaii.photobooth.domain.partner.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 제휴 업체 정보 및 쿠폰. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "partners")
public class Partner extends BaseEntity {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(name = "short_description", length = 200)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** 매장 로고 이미지 */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** 매장 대표 사진 */
    @Column(name = "thumbnail_image_url", length = 500)
    private String thumbnailImageUrl;

    /** 매장 사진 */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 길찾기 제공 방식 (MAP_URL: 네이버 지도 링크, WALK_GIF: 도보 경로 GIF) */
    @Enumerated(EnumType.STRING)
    @Column(name = "direction_type", length = 20, nullable = false)
    private DirectionType directionType;

    /** direction_type이 MAP_URL이면 네이버 지도 URL, WALK_GIF이면 GIF의 S3 URL */
    @Column(name = "direction_url", length = 500)
    private String directionUrl;

    /** 실제 쿠폰 혜택 내용 (예: 아메리카노 1잔 무료) */
    @Column(name = "coupon_description", length = 200)
    private String couponDescription;

    /** 업체 증감 시 삭제 대신 비활성화 처리하는 플래그 */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /**
     * 최근 당첨 순번(전역 시퀀스). 랜덤 배정 시 이 값이 가장 큰(=가장 최근 당첨된) 업체 1곳만 후보에서 제외하는 데 사용.
     * 특정 업체가 연속으로 당첨되는 상황을 최소화하는 게 목적이다.
     */
    @Column(name = "last_assigned_seq")
    private Long lastAssignedSeq;

    public static Partner of(
            String name,
            String location,
            String shortDescription,
            String description,
            String logoUrl,
            String thumbnailImageUrl,
            String imageUrl,
            DirectionType directionType,
            String directionUrl,
            String couponDescription
    ) {
        Partner partner = new Partner();
        partner.name = name;
        partner.location = location;
        partner.shortDescription = shortDescription;
        partner.description = description;
        partner.logoUrl = logoUrl;
        partner.thumbnailImageUrl = thumbnailImageUrl;
        partner.imageUrl = imageUrl;
        partner.directionType = directionType;
        partner.directionUrl = directionUrl;
        partner.couponDescription = couponDescription;
        partner.active = true;
        return partner;
    }

    /** 전역 시퀀스에서 발급받은 다음 순번을 최근 당첨 순번으로 기록한다. */
    public void assignNow(long nextSeq) {
        this.lastAssignedSeq = nextSeq;
    }

    public void deactivate() {
        this.active = false;
    }
}
