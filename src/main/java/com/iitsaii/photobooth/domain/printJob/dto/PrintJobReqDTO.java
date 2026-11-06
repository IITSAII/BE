package com.iitsaii.photobooth.domain.printJob.dto;

import com.iitsaii.photobooth.domain.printJob.entity.FilterType;
import com.iitsaii.photobooth.domain.printJob.entity.FrameType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class PrintJobReqDTO {

    @Schema(description = "프레임 선택 요청")
    public record FrameSelect(

            @Schema(
                    description = "선택한 프레임 종류",
                    example = "DARK"
            )
            @NotNull(message = "프레임은 필수입니다.")
            FrameType frameType,

            @Schema(description = "흑백 필터 적용 여부", example = "false")
            boolean filterBw,

            @Schema(description = "밝기 조절 값 (-100 ~ 100)", example = "75")
            @NotNull(message = "밝기 값은 필수입니다.")
            @Min(-100)
            @Max(100)
            Integer filterBrightness,

            @Schema(description = "선택한 필터. 없으면 filterBw로 정한다.", example = "VINTAGE")
            FilterType filterType
    ) {

        /** filterType을 보내지 않는 이전 FE도 동작하도록 filterBw로 대체한다. */
        public FilterType resolvedFilterType() {
            if (filterType != null) {
                return filterType;
            }
            return filterBw ? FilterType.GRAYSCALE : FilterType.DEFAULT;
        }
    }

    @Schema(description = "최종 인쇄 이미지 업로드 요청")
    public record UploadFinalImage(
            @NotNull
            @Schema(description = "프레임과 필터가 적용된 최종 4컷 이미지 파일")
            MultipartFile finalImage
    ) {}
}
