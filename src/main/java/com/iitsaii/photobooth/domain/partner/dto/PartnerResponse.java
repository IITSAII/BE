package com.iitsaii.photobooth.domain.partner.dto;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import com.iitsaii.photobooth.domain.session.entity.Session;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "세션에 배정된 제휴 업체 정보 응답")
public record PartnerResponse(

        @Schema(description = "매장 로고 이미지 URL")
        String logoUrl,

        @Schema(description = "매장 이름", example = "overnook")
        String name,

        @Schema(description = "매장 위치(주소)", example = "섭골길59 이편한세상아파트 근린상가 104호")
        String location,

        @Schema(description = "실제 쿠폰 혜택 내용", example = "아메리카노 1잔 무료")
        String couponDescription,

        @Schema(description = "인화물 QR/바코드용 세션 갤러리 토큰")
        UUID galleryToken
) {

    public static PartnerResponse of(Partner partner, Session session) {
        return new PartnerResponse(
                partner.getLogoUrl(),
                partner.getName(),
                partner.getLocation(),
                partner.getCouponDescription(),
                session.getGalleryToken()
        );
    }
}
