package com.iitsaii.photobooth.domain.partner.dto;

import com.iitsaii.photobooth.domain.partner.entity.DirectionType;
import com.iitsaii.photobooth.domain.partner.entity.Partner;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매거진 페이지에 노출할 제휴 업체 정보 응답")
public record PartnerListResponse(

        @Schema(description = "업체 식별자", example = "1")
        Long id,

        @Schema(description = "매장 이름", example = "OO커피")
        String name,

        @Schema(description = "매장 위치(주소)", example = "서울시 강남구 ...")
        String location,

        @Schema(description = "매장 부설명", example = "감성 가득한 동네 카페")
        String shortDescription,

        @Schema(description = "매장 상세설명")
        String description,

        @Schema(description = "매장 대표 사진 URL")
        String thumbnailImageUrl,

        @Schema(description = "매장 상세 사진 URL")
        String imageUrl,

        @Schema(description = "길찾기 제공 방식: MAP_URL(네이버 지도 링크) 또는 WALK_GIF(도보 경로 GIF)",
                example = "MAP_URL")
        DirectionType directionType,

        @Schema(description = "direction_type이 MAP_URL이면 네이버 지도 URL, WALK_GIF이면 GIF의 S3 URL")
        String directionUrl,

        @Schema(description = "실제 쿠폰 혜택 내용", example = "아메리카노 1잔 무료")
        String couponDescription
) {

    public static PartnerListResponse from(Partner partner) {
        return new PartnerListResponse(
                partner.getId(),
                partner.getName(),
                partner.getLocation(),
                partner.getShortDescription(),
                partner.getDescription(),
                partner.getThumbnailImageUrl(),
                partner.getImageUrl(),
                partner.getDirectionType(),
                partner.getDirectionUrl(),
                partner.getCouponDescription()
        );
    }
}
