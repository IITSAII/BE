package com.iitsaii.photobooth.domain.partner.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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

    /** 최근 당첨 시각. 랜덤 배정 시 최근 당첨 업체를 후순위로 미루는 데 사용 */
    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    public static Partner of(
            String name,
            String location,
            String shortDescription,
            String description,
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
        partner.imageUrl = imageUrl;
        partner.directionType = directionType;
        partner.directionUrl = directionUrl;
        partner.couponDescription = couponDescription;
        partner.active = true;
        return partner;
    }

    public void assignNow() {
        this.lastAssignedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
    }
}
