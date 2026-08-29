package com.iitsaii.photobooth.domain.partner.error;

import com.iitsaii.photobooth.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PartnerErrorCode implements ErrorCode {

    NO_ACTIVE_PARTNER(HttpStatus.NOT_FOUND, "PARTNER_404_1", "배정 가능한 제휴 업체가 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "PARTNER_404_2", "제휴 업체를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
