package com.iitsaii.photobooth.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "결제 승인 요청")
public record PaymentConfirmRequest(

        @Schema(description = "토스 결제창에서 발급된 결제 키", example = "5EnNZRJGvaBX7zk2yd8ydw26XvwXkLrx9POLqKQjmAw33nMv")
        @NotBlank(message = "paymentKey는 필수입니다.")
        String paymentKey,

        @Schema(description = """
                프론트가 토스 결제창을 열 때 사용한 결제 금액. 서버가 알고 있는 세션 금액과
                다르면 결제 승인을 거부한다 (금액 조작 방지).
                """, example = "6000")
        @NotNull(message = "amount는 필수입니다.")
        Integer amount
) {
}
