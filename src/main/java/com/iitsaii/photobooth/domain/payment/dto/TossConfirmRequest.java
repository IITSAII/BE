package com.iitsaii.photobooth.domain.payment.dto;

/** 토스페이먼츠 결제 승인 API 요청 바디. */
public record TossConfirmRequest(String paymentKey, String orderId, Long amount) {
}
