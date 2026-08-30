package com.iitsaii.photobooth.domain.partner.dto;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매거진 페이지에 노출할 제휴 업체 정보 응답")
public record PartnerListResponse(

        @Schema(description = "업체 식별자. 1은 잇사이(우리 서비스) 소개 카드를 뜻하는 고정값이며, 실제 Partner는 id 2부터 시작한다.", example = "1")
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

        @Schema(description = "네이버 지도 길찾기 URL")
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
                partner.getDirectionUrl(),
                partner.getCouponDescription()
        );
    }

    /**
     * 잇사이(우리 서비스) 소개용 더미 카드. DB의 Partner가 아니라 목록 API에서 프론트에 고정으로
     * 얹어주는 항목이다. id는 1L 고정값이며, 실제 Partner 테이블의 id 시퀀스를 2부터 시작하도록
     * DB에서 예약해뒀기 때문에 앞으로도 겹치지 않는다 (partners 테이블에는 id=1인 행이 존재하지 않음).
     * 실제 소개 문구/사진/위치 확정되면 이 상수만 갱신하면 된다.
     */
    public static PartnerListResponse ourStudio() {
        return new PartnerListResponse(
                1L,
                "잇사이",
                "세종특별자치시 조치원읍 세종로 2639 홍익대학교 세종캠퍼스",
                "우리가 만드는 순간, 잇사이",
                "잇사이는 소중한 사람과의 순간을 사진으로 남기는 포토부스 서비스입니다.",
                null,
                null,
                null,
                null
        );
    }
}
