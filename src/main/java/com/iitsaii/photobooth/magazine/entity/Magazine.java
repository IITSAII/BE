package com.iitsaii.photobooth.magazine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 제휴 업체 정보 및 쿠폰. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "magazines")
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "naver_map_url", length = 500)
    private String naverMapUrl;

    /** 실제 쿠폰 혜택 내용 (예: 아메리카노 1잔 무료) */
    @Column(name = "coupon_description", length = 200)
    private String couponDescription;

    /** 업체 증감 시 삭제 대신 비활성화 처리하는 플래그 */
    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 최근 당첨 시각. 랜덤 배정 시 최근 당첨 업체를 후순위로 미루는 데 사용 */
    @Column(name = "last_assigned_at")
    private LocalDateTime lastAssignedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
