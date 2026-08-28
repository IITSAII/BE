package com.iitsaii.photobooth.domain.payment.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 토스페이먼츠 결제 정보. 비회원(ANONYMOUS) 결제로 처리한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    /** 연결된 세션 (sessions.id 참조) */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * 토스 결제 요청/승인에 사용하는 주문 식별자. Session.sessionId를 그대로 재사용한다 (별도 발급하지 않음).
     * 영문 대소문자/숫자/-/_, 6~64자 (토스 규격) - sessionId 포맷이 이미 이 규격을 만족한다.
     */
    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId;

    /** 토스페이먼츠 발급 고유 결제 키 (중복 승인 방지) */
    @Column(name = "payment_key", length = 200, nullable = false, unique = true)
    private String paymentKey;

    /** 결제 수단 (토스페이, 카드, 계좌이체 등) */
    @Column(length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 토스 결제 승인 응답을 받은 시점에만 생성한다 (준비 단계 별도 row 없음). */
    public static Payment approved(Long sessionId, String orderId, String paymentKey, String method,
            LocalDateTime approvedAt) {
        Payment payment = new Payment();
        payment.sessionId = sessionId;
        payment.orderId = orderId;
        payment.paymentKey = paymentKey;
        payment.method = method;
        payment.status = PaymentStatus.DONE;
        payment.approvedAt = approvedAt;
        return payment;
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
