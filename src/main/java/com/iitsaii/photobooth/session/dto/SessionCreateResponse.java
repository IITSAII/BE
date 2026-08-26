package com.iitsaii.photobooth.session.dto;

import com.iitsaii.photobooth.session.entity.Session;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "세션 생성 응답")
public record SessionCreateResponse(

        @Schema(description = """
                생성된 세션의 공개 식별자. 프론트가 이후 상태 조회, 결제 등
                모든 후속 요청에서 이 값을 사용하며, 토스페이먼츠 orderId로도 그대로 사용된다.
                """, example = "sess_18f65b95c4fb")
        String sessionId,

        @Schema(description = "요청받은 quantity 기준으로 서버가 계산해 확정한 결제 금액(원)", example = "6000")
        Integer amount,

        @Schema(description = "세션 생성 직후 진입하는 다음 단계. 항상 PAYMENT로 시작한다.", example = "PAYMENT")
        String currentStep
) {

    public static SessionCreateResponse from(Session session) {
        return new SessionCreateResponse(
                session.getSessionId(),
                session.getAmount(),
                session.getCurrentStep().name()
        );
    }
}
