package com.iitsaii.photobooth.domain.payment.error;

import com.iitsaii.photobooth.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_400_1", "결제 승인에 실패했습니다."),
    AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "PAYMENT_400_2", "요청 금액이 세션의 결제 금액과 일치하지 않습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
