package com.iitsaii.photobooth.session.dto;

import com.iitsaii.photobooth.session.entity.Session;

public record SessionCreateResponse(
        String sessionId,
        Integer amount,
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
