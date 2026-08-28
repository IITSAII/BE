package com.iitsaii.photobooth.domain.photo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PhotoReqDTO {

    @Schema(description = "사진 선택 요청")
    public record SelectPhotos(

            @Size(min = 4, max = 4, message = "사진은 정확히 4장 선택해야 합니다.")
            @Schema(
                    description = "사용자가 최종 선택한 사진 ID 목록 (4개)",
                    example = "[1, 3, 4, 6]"
            )
            List<Long> photoIds
    ) {}
}
