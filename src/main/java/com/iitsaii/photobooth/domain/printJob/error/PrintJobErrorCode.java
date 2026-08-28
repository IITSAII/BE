package com.iitsaii.photobooth.domain.printJob.error;

import com.iitsaii.photobooth.global.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PrintJobErrorCode implements ErrorCode {

    PRINT_JOB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "PRINT404_1",
            "인쇄 작업을 찾을 수 없습니다."
    ),
    INVALID_FRAME_STEP(
            HttpStatus.BAD_REQUEST,
            "PRINT400_1",
            "현재 프레임 선택 단계가 아닙니다."
    ),
    FRAME_ALREADY_SELECTED(
            HttpStatus.BAD_REQUEST,
            "PRINT400_2",
            "이미 프레임이 선택되었습니다."
    ),
    INVALID_FRAME_TYPE(
            HttpStatus.BAD_REQUEST,
            "PRINT400_3",
            "지원하지 않는 프레임입니다."
    ),
    INVALID_BRIGHTNESS(
            HttpStatus.BAD_REQUEST,
            "PRINT400_4",
            "밝기 값은 -100 ~ 100 사이여야 합니다."
    ),
    FINAL_IMAGE_NOT_READY(
            HttpStatus.BAD_REQUEST,
            "PRINT400_5",
            "최종 인쇄 이미지가 아직 생성되지 않았습니다."
    ),
    PRINT_ALREADY_DONE(
            HttpStatus.BAD_REQUEST,
            "PRINT400_6",
            "이미 인쇄가 완료된 작업입니다."
    ),
    INVALID_IMAGE_FILE(
            HttpStatus.BAD_REQUEST,
            "PRINT400_7",
            "올바른 이미지 파일만 업로드할 수 있습니다."
    ),
    INVALID_PRINT_STATUS(
            HttpStatus.BAD_REQUEST,
            "PRINT400_8",
            "현재 인쇄 작업 상태에서는 요청을 처리할 수 없습니다."
    ),
    NO_PRINT_JOB_IN_QUEUE(
            HttpStatus.NO_CONTENT,
            "PRINT404_2",
            "출력 대기 중인 작업이 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
