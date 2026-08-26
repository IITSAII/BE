package com.iitsaii.photobooth.domain.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "세션 생성 요청")
public record SessionCreateRequest(

        @Schema(description = """
                촬영 수량 (필수). 2, 4, 6 중 하나만 허용된다.
                이 값으로 서버가 결제 금액을 계산해 확정하므로, 클라이언트는 금액을 별도로 보내지 않는다.
                목록에 없는 값이거나 생략하면 400(INVALID_QUANTITY)이 응답된다.
                """, example = "4")
        @NotNull(message = "촬영 수량은 필수입니다.")
        Integer quantity
) {
}
