package com.iitsaii.photobooth.session.dto;

import com.iitsaii.photobooth.session.entity.Session;
import java.time.LocalDateTime;

public record SessionStatusResponse(
        String sessionId,
        String status,
        String currentStep,
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
