package com.iitsaii.photobooth.session.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.session.entity.SessionStatus;
import com.iitsaii.photobooth.session.entity.SessionStep;
import com.iitsaii.photobooth.session.error.SessionErrorCode;
import com.iitsaii.photobooth.session.service.SessionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SessionService sessionService;

    @Test
    @DisplayName("POST /api/sessions - 정상 요청이면 200과 생성된 세션 정보를 반환한다")
    void createSession_success() throws Exception {
        given(sessionService.createSession(4))
                .willReturn(new SessionCreateResponse("sess_abc123", 6000, SessionStep.PAYMENT.name()));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuantityRequest(4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("sess_abc123"))
                .andExpect(jsonPath("$.data.amount").value(6000))
                .andExpect(jsonPath("$.data.currentStep").value("PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/sessions - quantity가 없으면 400을 반환한다")
    void createSession_missingQuantity() throws Exception {
        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/sessions - 지원하지 않는 수량이면 400과 에러 코드를 반환한다")
    void createSession_invalidQuantity() throws Exception {
        given(sessionService.createSession(5))
                .willThrow(new CustomException(SessionErrorCode.INVALID_QUANTITY));

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuantityRequest(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(SessionErrorCode.INVALID_QUANTITY.getCode()));
    }

    @Test
    @DisplayName("GET /api/sessions/{sessionId}/status - 존재하는 세션이면 200과 상태를 반환한다")
    void getStatus_success() throws Exception {
        given(sessionService.getStatus("sess_abc123"))
                .willReturn(new SessionStatusResponse(
                        "sess_abc123", SessionStatus.PAID.name(), SessionStep.RELATIONSHIP.name(), null));

        mockMvc.perform(get("/api/sessions/{sessionId}/status", "sess_abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.currentStep").value("RELATIONSHIP"));
    }

    @Test
    @DisplayName("GET /api/sessions/{sessionId}/status - 존재하지 않는 세션이면 404를 반환한다")
    void getStatus_notFound() throws Exception {
        given(sessionService.getStatus("sess_none"))
                .willThrow(new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        mockMvc.perform(get("/api/sessions/{sessionId}/status", "sess_none"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value(SessionErrorCode.SESSION_NOT_FOUND.getCode()));
    }

    private record QuantityRequest(Integer quantity) {
    }
}
