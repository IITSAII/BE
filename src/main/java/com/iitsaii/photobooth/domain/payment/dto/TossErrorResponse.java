package com.iitsaii.photobooth.domain.payment.dto;

/** 토스페이먼츠 API 에러 응답 바디. */
public record TossErrorResponse(String code, String message) {
}
