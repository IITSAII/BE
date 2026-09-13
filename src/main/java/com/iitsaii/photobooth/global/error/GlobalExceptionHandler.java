package com.iitsaii.photobooth.global.error;

import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.global.common.CommonResponse;
import io.sentry.Sentry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 우리 코드는 정상인데 외부 장애(토스 결제 게이트웨이 등)로 발생한 502성 CustomException.
     * 일반 4xx CustomException(잘못된 요청, 유효성 검증 실패 등)은 노이즈라 Sentry에 보내지 않고,
     * 이 목록에 해당하는 경우만 선별적으로 캡처한다.
     */
    private static final Set<ErrorCode> ALERT_WORTHY_ERROR_CODES = Set.of(
            PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
            PaymentErrorCode.PAYMENT_CANCEL_FAILED
    );

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<CommonResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {}", e.getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        if (ALERT_WORTHY_ERROR_CODES.contains(errorCode)) {
            Sentry.captureException(e);
        }
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(CommonResponse.error(errorCode, e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<CommonResponse<Void>> handleInvalidRequest(Exception e) {
        log.warn("Invalid request: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMessage());
        return ResponseEntity.status(GlobalErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorCode.METHOD_NOT_ALLOWED));
    }

    /**
     * 존재하지 않는 정적 리소스/경로 요청 (봇, 스캐너, 헬스체크 등이 루트(`/`)나 없는 경로로 보내는
     * 노이즈성 요청 포함). 우리 코드/의존성 버그가 아니므로 Sentry에는 보내지 않고 조용히 404만 반환한다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFound(NoResourceFoundException e) {
        return ResponseEntity.status(GlobalErrorCode.ENTITY_NOT_FOUND.getHttpStatus())
                .body(CommonResponse.error(GlobalErrorCode.ENTITY_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        Sentry.captureException(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR));
    }
}
