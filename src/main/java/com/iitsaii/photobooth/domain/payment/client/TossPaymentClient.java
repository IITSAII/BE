package com.iitsaii.photobooth.domain.payment.client;

import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.dto.TossErrorResponse;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.global.error.CustomException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** 토스페이먼츠 결제 승인 API 연동 클라이언트. */
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient;

    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .build();
    }

    /**
     * 결제 승인을 요청한다.
     * 토스페이먼츠가 4xx/5xx로 응답하면 {@link PaymentErrorCode#PAYMENT_CONFIRM_FAILED}로 변환한다.
     */
    public TossConfirmResponse confirm(TossConfirmRequest request) {
        try {
            return restClient.post()
                    .uri(CONFIRM_URL)
                    .body(request)
                    .retrieve()
                    .body(TossConfirmResponse.class);
        } catch (RestClientResponseException e) {
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED, extractMessage(e));
        }
    }

    private String extractMessage(RestClientResponseException e) {
        try {
            TossErrorResponse errorResponse = e.getResponseBodyAs(TossErrorResponse.class);
            return errorResponse != null ? errorResponse.message() : PaymentErrorCode.PAYMENT_CONFIRM_FAILED.getMessage();
        } catch (Exception parseFailure) {
            return PaymentErrorCode.PAYMENT_CONFIRM_FAILED.getMessage();
        }
    }
}
