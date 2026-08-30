package com.iitsaii.photobooth.domain.payment.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.global.error.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TossPaymentClientTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
    private final TossPaymentClient tossPaymentClient = new TossPaymentClient(builder);

    @Test
    @DisplayName("결제 승인 요청 시 orderId를 Idempotency-Key 헤더로 함께 보낸다")
    void sendsIdempotencyKeyHeaderOnConfirm() {
        TossConfirmRequest request = new TossConfirmRequest("payment-key", "sess_abc123", 6000L);

        mockServer.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Idempotency-Key", "sess_abc123"))
                .andRespond(withSuccess(
                        """
                        {"paymentKey":"payment-key","orderId":"sess_abc123","status":"DONE","method":"카드","approvedAt":"2026-08-28T10:00:00+09:00"}
                        """,
                        MediaType.APPLICATION_JSON));

        TossConfirmResponse response = tossPaymentClient.confirm(request);

        assertThat(response.orderId()).isEqualTo("sess_abc123");
        mockServer.verify();
    }

    @Test
    @DisplayName("5xx 응답이면 PAYMENT_GATEWAY_UNAVAILABLE로 변환한다")
    void mapsServerErrorToGatewayUnavailable() {
        TossConfirmRequest request = new TossConfirmRequest("payment-key", "sess_abc123", 6000L);

        mockServer.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(header("Idempotency-Key", "sess_abc123"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> tossPaymentClient.confirm(request))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE);
        mockServer.verify();
    }
}
