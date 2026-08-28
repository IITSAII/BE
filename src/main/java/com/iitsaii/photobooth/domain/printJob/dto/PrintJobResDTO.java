package com.iitsaii.photobooth.domain.printJob.dto;

import com.iitsaii.photobooth.domain.printJob.entity.FrameType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class PrintJobResDTO {

    @Builder
    @Schema(description = "프레임 선택 응답")
    public record FrameSelect(

            @Schema(description = "생성된 Print Job ID", example = "1")
            Long printJobId,

            @Schema(description = "선택된 프레임", example = "DARK")
            FrameType frameType
    ) {}
}
