package com.iitsaii.photobooth.domain.partner.dto;

import com.iitsaii.photobooth.domain.partner.entity.Partner;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "매거진 페이지에 노출할 제휴 업체 정보 응답")
public record PartnerListResponse(

        @Schema(description = "업체 식별자. 잇사이(우리 서비스) 소개 카드는 실제 Partner가 아니므로 null이다.", example = "1")
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
        String couponDescription,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "협약 참가자 이름 목록")
        List<String> participantNames,

        @Schema(description = "잇사이(우리 서비스) 소개 카드 여부. true면 실제 Partner가 아니라 프론트 고정 노출용 카드다.")
        boolean isOurStudio
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
                partner.getCouponDescription(),
                partner.getProfileImageUrl(),
                partner.getParticipantNames(),
                false
        );
    }

    /**
     * 잇사이(우리 서비스) 소개용 더미 카드. DB의 Partner가 아니라 목록 API에서 프론트에 고정으로
     * 얹어주는 항목이다. 실제 Partner가 아니므로 id는 null이며, isOurStudio 플래그로 구분한다.
     */
    public static PartnerListResponse ourStudio() {
        return new PartnerListResponse(
                null,
                "잇사이",
                null,
                "평범한 일상 속 소중함을 찾아 잇는, 포토 부스 잇, 사이의 이야기",
                "휘발되기 쉬운 기억, 스쳐 지나가는 순간, 잊고 싶지 않은  추억, <잇, 사이>는 조치원의 소중한 순간들을 사진이라는 매개체를 통해 오래도록 마음속에 머물게 하는 관계 기반  셀프 포토 부스입니다. 누군가와 함께한 특별한 순간부터  평범한 하루의 한 장면까지, 사진에 담긴 기억은 시간이  지나도 그날의 순간을 다시 꺼내 볼 수 있게 합니다. 그리고 그렇게 남겨진 사진은 한 사람의 기록에 머무르지 않고, 같은 지역을 살아가는 사람들의 서로 다른 순간과 경험을  연결하는 매개가 됩니다. ‘사이를 잇다’라는 의미처럼, <잇, 사이>는 서로 연결되지 않으면 쉽게 멀어지고 끊어질 수 있는 관계를 사진을 통해 이어가고자 합니다. 대학 생활 속 소중한 순간을 기록할 수 있도록 함께 사진을 찍은 사람과의 관계와 촬영 날짜를 사진에  남기고, 관계에 따라 더욱 가까워질 수 있는 포즈를 추천하는 서비스를 제공합니다. 더 나아가 잘 알려지지 않았던  조치원의 다양한 로컬 프로젝트 사이를 연결하고 새로운  경험을 발견할 수 있도록 <마주하다>, <피치못한>, <반짝>, 과 협업하여 제휴 서비스를 제공합니다. 개인의 순간을 기록하는 것에서 시작해 사람과 사람 사이를, 그리고 지역과 지역 사이의 관계를 이어가는 것.  <잇, 사이>는 사진 한 장을 통해 일상의 기억과 새로운 관계가 이어지는 경험을 만들어 갑니다.",
                "https://iitsaii-photobooth-images.s3.ap-northeast-2.amazonaws.com/partners/itsai/thumbnail.svg",
                null,
                null,
                null,
                "https://iitsaii-photobooth-images.s3.ap-northeast-2.amazonaws.com/partners/itsai/profile.svg",
                null,
                true
        );
    }
}
