package com.iitsaii.photobooth.domain.payment.dto;

import java.time.OffsetDateTime;

/** 토스페이먼츠 결제 승인 API 응답 바디 (필요한 필드만 매핑). */
public record TossConfirmResponse(
        String paymentKey,
        String orderId,
        String status,
        String method,
        OffsetDateTime approvedAt,
        Long totalAmount
) {
}
