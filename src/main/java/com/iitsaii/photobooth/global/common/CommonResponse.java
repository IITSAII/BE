package com.iitsaii.photobooth.global.common;

import com.iitsaii.photobooth.global.error.ErrorCode;

public record CommonResponse<T>(
        boolean success,
        T data,
        ErrorDetail error
) {

    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>(true, data, null);
    }

    public static CommonResponse<Void> ok() {
        return new CommonResponse<>(true, null, null);
    }

    public static CommonResponse<Void> error(ErrorCode errorCode) {
        return new CommonResponse<>(false, null, ErrorDetail.of(errorCode));
    }

    public static CommonResponse<Void> error(ErrorCode errorCode, String message) {
        return new CommonResponse<>(false, null, new ErrorDetail(errorCode.getCode(), message));
    }

    public record ErrorDetail(String code, String message) {

        public static ErrorDetail of(ErrorCode errorCode) {
            return new ErrorDetail(errorCode.getCode(), errorCode.getMessage());
        }
    }
}
