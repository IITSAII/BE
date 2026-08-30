package com.iitsaii.photobooth.domain.payment.client;

import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.dto.TossErrorResponse;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.global.error.CustomException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** 토스페이먼츠 결제 승인 API 연동 클라이언트. */
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
    private static final String ORDER_QUERY_URL = "https://api.tosspayments.com/v1/payments/orders/{orderId}";
    private static final String APPROVED_STATUS = "DONE";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    /** 토스 서버가 응답을 안 주는 상황에서 요청 스레드가 무한정 블로킹되지 않도록 명시적으로 설정한다. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private final RestClient restClient;

    @Autowired
    public TossPaymentClient(@Value("${toss.secret-key}") String secretKey) {
        this(defaultBuilder(secretKey));
    }

    /** 테스트에서 {@code MockRestServiceServer}를 붙일 수 있도록 builder를 직접 주입받는 패키지 전용 생성자. */
    TossPaymentClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    private static RestClient.Builder defaultBuilder(String secretKey) {
        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
    }

    /**
     * 결제 승인을 요청한다.
     * 토스페이먼츠가 4xx로 응답하면 {@link PaymentErrorCode#PAYMENT_CONFIRM_FAILED}(400)로,
     * 5xx로 응답하면(토스 쪽 장애) {@link PaymentErrorCode#PAYMENT_GATEWAY_UNAVAILABLE}(502)로 구분해서 변환한다.
     * 네트워크 자체가 실패(타임아웃, 연결 불가 등)하면 이 메서드에서 잡지 않고 그대로 전파한다
     * (GlobalExceptionHandler의 500 처리로 넘어가며, 우리 쪽 요청 문제가 아니므로 400/502로 감추지 않는다).
     *
     * orderId(=sessionId)를 Idempotency-Key로 함께 보내서, 응답 유실 등으로 우리 쪽에서 같은 요청을
     * 재시도하더라도 토스 쪽에서 중복 승인이 발생하지 않도록 한다. 세션당 결제 승인은 항상 1회만
     * 일어나므로 별도 키 발급 없이 orderId를 그대로 재사용한다.
     */
    public TossConfirmResponse confirm(TossConfirmRequest request) {
        try {
            return restClient.post()
                    .uri(CONFIRM_URL)
                    .header(IDEMPOTENCY_KEY_HEADER, request.orderId())
                    .body(request)
                    .retrieve()
                    .body(TossConfirmResponse.class);
        } catch (RestClientResponseException e) {
            PaymentErrorCode errorCode = e.getStatusCode().is5xxServerError()
                    ? PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE
                    : PaymentErrorCode.PAYMENT_CONFIRM_FAILED;
            throw new CustomException(errorCode, extractMessage(e));
        }
    }

    /**
     * orderId로 결제를 조회해서 실제로 승인(DONE)됐는지 확인한다.
     * 승인 요청이 타임아웃/5xx로 실패했을 때, "토스는 처리했는데 응답만 못 받은" 상황인지
     * 재확인하는 용도로 사용한다. 승인된 적이 없거나(404) 아직 DONE이 아니면 빈 값을 반환한다.
     * 이 재조회 자체가 실패(타임아웃, 연결 불가 등 {@link RestClientException})해도 예외를 전파하지 않고
     * 빈 값을 반환한다 - 호출부가 원래 예외로 폴백할 수 있도록 한다.
     */
    public Optional<TossConfirmResponse> findApprovedByOrderId(String orderId) {
        try {
            TossConfirmResponse response = restClient.get()
                    .uri(ORDER_QUERY_URL, orderId)
                    .retrieve()
                    .body(TossConfirmResponse.class);
            return (response != null && APPROVED_STATUS.equals(response.status()))
                    ? Optional.of(response)
                    : Optional.empty();
        } catch (RestClientException e) {
            return Optional.empty();
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
