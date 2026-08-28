package com.iitsaii.photobooth.domain.photo.error;

import com.iitsaii.photobooth.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PhotoErrorCode implements ErrorCode {

    INVALID_SHOT_NUMBER(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_1",
            "촬영 순서는 1~6만 가능합니다."
    ),
    PHOTO_LIMIT_EXCEEDED(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_2",
            "촬영 사진은 최대 6장까지 저장할 수 있습니다."
    ),
    INVALID_CAPTURE_STEP(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_3",
            "현재 촬영 가능한 단계가 아닙니다."
    ),
    PHOTO_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "PHOTO409_1",
            "이미 저장된 촬영 컷입니다."
    ),
    INVALID_SELECT_STEP(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_4",
            "현재 사진을 선택할 수 있는 단계가 아닙니다."
    ),
    INVALID_SELECTED_COUNT(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_5",
            "사진은 정확히 4장 선택해야 합니다."
    ),
    DUPLICATE_SELECTED_PHOTO(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_6",
            "같은 사진을 중복 선택할 수 없습니다."
    ),
    INVALID_SELECTED_PHOTO(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_7",
            "현재 세션의 사진만 선택할 수 있습니다."
    ),
    PHOTO_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PHOTO404_1",
            "선택한 사진을 찾을 수 없습니다."
    ),
    PHOTOS_ALREADY_SELECTED(
            HttpStatus.CONFLICT,
            "PHOTO409_2",
            "이미 사진 선택이 완료된 세션입니다."
    ),
    EMPTY_IMAGE(
            HttpStatus.BAD_REQUEST,
            "PHOTO400_8",
            "업로드할 이미지가 없습니다."
    ),
    PHOTO_UPLOAD_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "PHOTO500_1",
            "사진 업로드에 실패했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
