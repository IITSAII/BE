package com.iitsaii.photobooth.domain.payment.dto;

/** 토스페이먼츠 결제 취소 API 응답 바디 (필요한 필드만 매핑). */
public record TossCancelResponse(
        String paymentKey,
        String orderId,
        String status
) {
}
