package com.iitsaii.photobooth.payment.entity;

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

    public static Payment of(Long sessionId, String paymentKey, String method) {
        Payment payment = new Payment();
        payment.sessionId = sessionId;
        payment.paymentKey = paymentKey;
        payment.method = method;
        payment.status = PaymentStatus.READY;
        return payment;
    }

    public void approve() {
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELED;
    }
}
