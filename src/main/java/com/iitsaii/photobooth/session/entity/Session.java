package com.iitsaii.photobooth.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 서비스 전체 흐름의 상태를 관리하는 허브 엔티티.
 * 수량 선택 시 생성되며, current_step / step_expires_at으로 단계 진행과 타임아웃을 관리한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이번 회차에 랜덤 당첨된 제휴 업체 (magazines.id 참조) */
    @Column(name = "magazine_id")
    private Long magazineId;

    /** 외부(토스 등) 연동 및 프론트 참조용 공개 식별자. FK로는 쓰이지 않음 */
    @Column(name = "session_id", length = 64, nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 20)
    private RelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 20)
    private SessionStep currentStep;

    @Column(name = "step_expires_at")
    private LocalDateTime stepExpiresAt;

    /** 인화물 QR/바코드용 토큰. 만료 없이 항상 사이트 접속 가능 */
    @Column(name = "gallery_token", nullable = false, updatable = false)
    private UUID galleryToken = UUID.randomUUID();

    @Column(name = "photo_view_expires_at")
    private LocalDateTime photoViewExpiresAt;

    @Column(name = "coupon_expires_at")
    private LocalDateTime couponExpiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
