package com.iitsaii.photobooth.domain.printJob.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.service.PrintJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = {PrintJobController.class, PrintQueueController.class},
        properties = "print-agent.token=test-token"
)
class PrintAgentAuthTest {

    private static final String TOKEN_HEADER = "X-Agent-Token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrintJobService printJobService;

    @ParameterizedTest
    @ValueSource(strings = {"done", "failed"})
    @DisplayName("PATCH 에이전트 API - 토큰이 없으면 401을 반환하고 서비스를 호출하지 않는다")
    void patchWithoutToken(String action) throws Exception {
        mockMvc.perform(patch("/api/sessions/sess_test/print/" + action))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("GLOBAL_401_1"));

        verifyNoInteractions(printJobService);
    }

    @Test
    @DisplayName("GET /api/print/queue - 토큰이 틀리면 401을 반환하고 작업을 꺼내지 않는다")
    void queueWithWrongToken() throws Exception {
        mockMvc.perform(get("/api/print/queue").header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(printJobService);
    }

    @Test
    @DisplayName("POST /print/retry - 토큰이 없으면 401을 반환한다")
    void retryWithoutToken() throws Exception {
        mockMvc.perform(post("/api/sessions/sess_test/print/retry"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(printJobService);
    }

    @Test
    @DisplayName("GET /api/print/queue - 토큰이 맞으면 작업을 반환한다")
    void queueWithValidToken() throws Exception {
        given(printJobService.getPrintQueue())
                .willReturn(PrintJobResDTO.PrintQueue.builder().sessionId("sess_test").build());

        mockMvc.perform(get("/api/print/queue").header(TOKEN_HEADER, "test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("sess_test"));
    }

    @Test
    @DisplayName("POST /print/retry - 토큰이 맞으면 재인쇄를 요청한다")
    void retryWithValidToken() throws Exception {
        mockMvc.perform(post("/api/sessions/sess_test/print/retry").header(TOKEN_HEADER, "test-token"))
                .andExpect(status().isOk());

        verify(printJobService).retryPrint("sess_test");
    }

    @Test
    @DisplayName("GET /print - FE가 쓰는 조회 API는 토큰 없이 호출할 수 있다")
    void printInfoWithoutToken() throws Exception {
        mockMvc.perform(get("/api/sessions/sess_test/print"))
                .andExpect(status().isOk());
    }
}
