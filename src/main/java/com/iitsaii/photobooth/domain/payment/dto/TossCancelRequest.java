package com.iitsaii.photobooth.domain.payment.dto;

/** 토스페이먼츠 결제 취소 API 요청 바디. */
public record TossCancelRequest(String cancelReason) {
}
