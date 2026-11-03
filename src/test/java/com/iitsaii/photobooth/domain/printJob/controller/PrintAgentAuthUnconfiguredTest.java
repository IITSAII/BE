package com.iitsaii.photobooth.domain.printJob.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iitsaii.photobooth.domain.printJob.service.PrintJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = PrintQueueController.class,
        properties = "print-agent.token="
)
class PrintAgentAuthUnconfiguredTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrintJobService printJobService;

    @Test
    @DisplayName("서버에 토큰이 설정되지 않으면 빈 토큰을 보내도 401을 반환한다")
    void rejectsEmptyTokenWhenUnconfigured() throws Exception {
        mockMvc.perform(get("/api/print/queue").header("X-Agent-Token", ""))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(printJobService);
    }
}
