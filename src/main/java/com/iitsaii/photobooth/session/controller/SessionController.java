package com.iitsaii.photobooth.session.controller;

import com.iitsaii.photobooth.global.common.CommonResponse;
import com.iitsaii.photobooth.session.dto.SessionCreateRequest;
import com.iitsaii.photobooth.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public CommonResponse<SessionCreateResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        return CommonResponse.ok(sessionService.createSession(request.quantity()));
    }

    @GetMapping("/{sessionId}/status")
    public CommonResponse<SessionStatusResponse> getStatus(@PathVariable String sessionId) {
        return CommonResponse.ok(sessionService.getStatus(sessionId));
    }
}
