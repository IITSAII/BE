package com.iitsaii.photobooth.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AgentAuthConfig implements WebMvcConfigurer {

    private final AgentTokenInterceptor agentTokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(agentTokenInterceptor)
                .addPathPatterns(
                        "/api/print/queue",
                        "/api/sessions/*/print/done",
                        "/api/sessions/*/print/failed",
                        "/api/sessions/*/print/retry"
                );
    }
}
