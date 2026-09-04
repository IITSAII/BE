package com.iitsaii.photobooth.domain.printJob.dto;

import com.iitsaii.photobooth.domain.printJob.entity.FrameType;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;

public class PrintJobResDTO {

    @Builder
    @Schema(description = "프레임 선택 응답")
    public record FrameSelect(

            @Schema(description = "생성된 Print Job ID", example = "1")
            Long printJobId,

            @Schema(description = "선택된 프레임", example = "DARK")
            FrameType frameType
    ) {}

    @Builder
    @Schema(description = "최종 인쇄 이미지 업로드 응답")
    public record UploadFinalImage(
            @Schema(description = "업로드된 최종 인쇄 이미지 URL")
            String finalImageUrl
    ) {}

    @Builder
    @Schema(description = "최종 인쇄 이미지 조회 응답")
    public record PrintInfo(

            @Schema(description = "최종 4컷 인쇄 이미지 URL", example = "https://iitsaii-photobooth-images.s3.ap-northeast-2.amazonaws.com/prints/sess_xxx/final.jpg")
            String finalImageUrl,

            @Schema(description = "선택한 프레임 종류", example = "DARK")
            FrameType frameType,

            @Schema(description = "흑백 필터 적용 여부", example = "false")
            Boolean filterBw,

            @Schema(description = "밝기 조절 값 (-100 ~ 100)", example = "20")
            Integer filterBrightness,

            @Schema(description = "인쇄 작업 상태", example = "QUEUED")
            PrintJobStatus status,

            @Schema(description = "촬영 날짜", example = "2026-09-04")
            LocalDate capturedAt
    ) {}

    @Builder
    @Schema(description = "인쇄 대기 작업 조회 응답")
    public record PrintQueue(

            @Schema(description = "세션 ID")
            String sessionId,

            @Schema(description = "최종 4컷 인쇄 이미지 URL", example = "https://iitsaii-photobooth-images.s3.ap-northeast-2.amazonaws.com/prints/sess_xxx/final.jpg")
            String finalImageUrl,

            @Schema(description = "선택한 프레임 종류", example = "DARK")
            FrameType frameType,

            @Schema(description = "흑백 필터 적용 여부", example = "false")
            Boolean filterBw,

            @Schema(description = "밝기 조절 값 (-100 ~ 100)", example = "20")
            Integer filterBrightness
    ) {}
}
