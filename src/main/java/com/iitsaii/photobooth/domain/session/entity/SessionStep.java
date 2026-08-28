package com.iitsaii.photobooth.domain.session.entity;

/** 현재 진행 단계 */
public enum SessionStep {
    QUANTITY,
    PAYMENT,
    RELATIONSHIP,
    CAPTURE,
    SELECT,
    FRAME,
    PRINT,
    DONE
}
