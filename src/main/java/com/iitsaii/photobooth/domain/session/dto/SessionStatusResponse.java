package com.iitsaii.photobooth.domain.session.dto;

import com.iitsaii.photobooth.domain.session.entity.Session;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "세션 상태 조회 응답. 프론트가 결제/타이머 상태 확인을 위해 주기적으로 폴링할 때 사용한다.")
public record SessionStatusResponse(

        @Schema(description = "조회한 세션의 공개 식별자", example = "sess_18f65b95c4fb")
        String sessionId,

        @Schema(description = "세션 전체 상태: CREATED, PAID, IN_PROGRESS, DONE, EXPIRED", example = "PAID")
        String status,

        @Schema(description = "현재 진행 단계: QUANTITY, PAYMENT, RELATIONSHIP, CAPTURE, SELECT, FRAME, PRINT, DONE",
                example = "RELATIONSHIP")
        String currentStep,

        @Schema(description = """
                현재 단계의 타임아웃 시각. 아직 진입한 적 없는 단계이거나 타임아웃이 설정되지 않은 단계면 null이다.
                이 시각을 지나면 프론트는 기본값으로 자동 진행된 것으로 간주하고 다시 상태를 조회해야 한다.
                """, example = "2026-08-27T14:31:00", nullable = true)
        LocalDateTime stepExpiresAt
) {

    public static SessionStatusResponse from(Session session) {
        return new SessionStatusResponse(
                session.getSessionId(),
                session.getStatus().name(),
                session.getCurrentStep().name(),
                session.getStepExpiresAt()
        );
    }
}
