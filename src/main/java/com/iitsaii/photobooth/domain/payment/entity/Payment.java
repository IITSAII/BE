package com.iitsaii.photobooth.domain.payment.entity;

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
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 세션 (sessions.id 참조) */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /**
     * 우리 상점이 발급하는 주문 식별자. 토스 결제 요청 시 함께 전달하며, 결제 상태가 바뀌어도 유지된다.
     * 영문 대소문자/숫자/-/_, 6~64자 (토스 규격).
     */
    @Column(name = "order_id", length = 64, nullable = false, unique = true)
    private String orderId;

    /** 토스페이먼츠 발급 고유 결제 키 (중복 승인 방지). 결제 준비 시점에는 알 수 없고, 승인 시 채워진다. */
    @Column(name = "payment_key", length = 200, unique = true)
    private String paymentKey;

    /** 결제 수단 (토스페이, 카드, 계좌이체 등). 승인 시 채워진다. */
    @Column(length = 30)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 결제 준비 단계. 결제창을 열기 전 orderId를 발급하며, paymentKey/method는 승인 시점에 채워진다. */
    public static Payment prepare(Long sessionId, String orderId) {
        Payment payment = new Payment();
        payment.sessionId = sessionId;
        payment.orderId = orderId;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    /** 토스 결제 승인 완료 처리. */
    public void approve(String paymentKey, String method) {
        this.paymentKey = paymentKey;
        this.method = method;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
