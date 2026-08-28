package com.iitsaii.photobooth.domain.payment.controller;

import com.iitsaii.photobooth.domain.payment.dto.PaymentConfirmRequest;
import com.iitsaii.photobooth.domain.payment.service.PaymentService;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions/{sessionId}/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "결제 승인",
            description = """
                    토스 결제창에서 결제를 완료한 뒤 프론트가 받은 paymentKey와 결제 금액으로 승인을 요청한다.
                    - orderId는 별도로 받지 않는다. 결제창을 열 때 이미 sessionId를 orderId로 사용했기 때문에,
                      서버가 경로의 sessionId를 그대로 orderId로 사용해 토스에 승인 요청을 보낸다.
                    - amount는 서버가 알고 있는 세션 금액과 대조해서, 다르면 토스 호출 없이 즉시 거부한다.
                    - 승인 성공 시 세션은 PAID로 전환되고 RELATIONSHIP 단계로 진입한다.
                    - PAYMENT 단계가 아닌 세션(이미 승인됐거나 아직 도달하지 않은 세션)에는 재요청할 수 없다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "PAYMENT 단계가 아니거나(`SessionErrorCode.INVALID_STEP`), " +
                    "금액 불일치(`PaymentErrorCode.AMOUNT_MISMATCH`), 토스 승인 실패(`PaymentErrorCode.PAYMENT_CONFIRM_FAILED`)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 세션 (`SessionErrorCode.SESSION_NOT_FOUND`)"),
    })
    @PostMapping("/confirm")
    public CommonResponse<SessionStatusResponse> confirm(
            @Parameter(description = "세션의 공개 식별자 (= 토스 orderId)", example = "sess_18f65b95c4fb")
            @PathVariable String sessionId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        return CommonResponse.ok(paymentService.confirm(sessionId, request.paymentKey(), request.amount()));
    }
}
