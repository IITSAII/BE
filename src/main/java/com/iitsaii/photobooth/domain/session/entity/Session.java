package com.iitsaii.photobooth.domain.session.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
public class Session extends BaseEntity {

    /** 이번 회차에 랜덤 당첨된 제휴 업체 (partners.id 참조) */
    @Column(name = "partner_id")
    private Long partnerId;

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

    /** 동시 요청으로 인한 단계 전이 덮어쓰기를 막기 위한 낙관적 락 버전 */
    @Version
    @Column(nullable = false)
    private Long version;

    /** 수량 선택(1단계)은 이미 완료된 상태로 호출되므로, 생성 직후 다음 단계인 결제로 진입한다. */
    public static Session of(String sessionId, Integer quantity, Integer amount) {
        Session session = new Session();
        session.sessionId = sessionId;
        session.quantity = quantity;
        session.amount = amount;
        session.status = SessionStatus.CREATED;
        session.currentStep = SessionStep.PAYMENT;
        return session;
    }

    public void advanceTo(SessionStep step, LocalDateTime stepExpiresAt) {
        this.currentStep = step;
        this.stepExpiresAt = stepExpiresAt;
    }

    /** RELATIONSHIP 단계 완료 처리. relationshipType이 null이면 "설정 안 함"으로 취급한다. */
    public void chooseRelationship(RelationshipType relationshipType, LocalDateTime nextStepExpiresAt) {
        this.relationshipType = relationshipType;
        advanceTo(SessionStep.CAPTURE, nextStepExpiresAt);
    }

    /** 결제 승인 완료 처리. 결제 상태로 전환하고 RELATIONSHIP 단계로 진입시킨다. */
    public void completePayment(LocalDateTime nextStepExpiresAt) {
        markPaid();
        advanceTo(SessionStep.RELATIONSHIP, nextStepExpiresAt);
    }

    public void markPaid() {
        this.status = SessionStatus.PAID;
    }

    public void markExpired() {
        this.status = SessionStatus.EXPIRED;
    }

    /**
     * PAYMENT 단계 타임아웃을 지연 평가(lazy)로 처리한다. 별도 배치/스케줄러 없이,
     * 세션에 접근하는 시점(상태 조회, 결제 승인 시도 등)마다 이 메서드로 만료 여부를 확인한다.
     * 결제는 기본값으로 대체 진행할 수 없는 단계라, 시간이 지나면 세션을 종료(EXPIRED)한다.
     */
    public void expireIfPaymentTimedOut(LocalDateTime now) {
        if (currentStep == SessionStep.PAYMENT
                && status != SessionStatus.EXPIRED
                && stepExpiresAt != null
                && now.isAfter(stepExpiresAt)) {
            markExpired();
        }
    }
}
