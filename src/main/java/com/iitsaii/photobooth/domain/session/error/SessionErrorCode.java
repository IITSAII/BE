package com.iitsaii.photobooth.domain.session.error;

import com.iitsaii.photobooth.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SessionErrorCode implements ErrorCode {

    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "SESSION_400_1", "지원하지 않는 촬영 수량입니다."),
    INVALID_STEP(HttpStatus.BAD_REQUEST, "SESSION_400_2", "현재 단계에서 수행할 수 없는 요청입니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_404_1", "세션을 찾을 수 없습니다."),
    CONCURRENT_REQUEST(HttpStatus.CONFLICT, "SESSION_409_1", "동시에 처리할 수 없는 요청입니다. 다시 시도해주세요."),
    SESSION_EXPIRED(HttpStatus.GONE, "SESSION_410_1", "세션이 만료되었습니다. 처음부터 다시 시작해주세요."),
    PARTNER_NOT_ASSIGNED(HttpStatus.NOT_FOUND, "SESSION_404_2", "아직 배정된 제휴 업체가 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
