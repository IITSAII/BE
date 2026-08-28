package com.iitsaii.photobooth.domain.photo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

public class PhotoResDTO {

    @Builder
    @Schema(description = "사진 업로드 응답")
    public record SavePhoto(

            @Schema(description = "저장된 사진 ID", example = "1")
            Long photoId,

            @Schema(description = "촬영 순서", example = "2")
            Integer shotNumber,

            @Schema(
                    description = "업로드된 사진의 URL",
                    example = "https://s3.ap-northeast-2.amazonaws.com/..."
            )
            String imageUrl
    ) {}

    @Builder
    @Schema(description = "사진 정보")
    public record PhotoInfo(

            @Schema(description = "사진 ID", example = "1")
            Long photoId,

            @Schema(description = "촬영 순서", example = "2")
            Integer shotNumber,

            @Schema(
                    description = "업로드된 사진의 URL",
                    example = "https://s3.ap-northeast-2.amazonaws.com/..."
            )
            String imageUrl
    ) {}

    @Builder
    @Schema(description = "세션의 사진 목록 조회 응답")
    public record PhotoList(

            @Schema(description = "촬영된 사진 목록")
            List<PhotoInfo> photos
    ) {}

    @Builder
    @Schema(description = "선택된 사진 정보")
    public record SelectedPhotoInfo(

            @Schema(description = "사진 ID", example = "1")
            Long photoId,

            @Schema(description = "선택 순서", example = "2")
            Integer selectOrder,

            @Schema(
                    description = "업로드된 사진의 URL",
                    example = "https://s3.ap-northeast-2.amazonaws.com/..."
            )
            String imageUrl
    ) {}

    @Builder
    @Schema(description = "세션의 선택된 사진 목록 조회 응답")
    public record SelectedPhotoList(

            @Schema(description = "선택된 사진 목록")
            List<SelectedPhotoInfo> photos
    ) {}
}
