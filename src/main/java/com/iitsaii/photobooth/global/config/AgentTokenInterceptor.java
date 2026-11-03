package com.iitsaii.photobooth.global.config;

import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.global.error.GlobalErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 맥북 프린트 에이전트만 호출할 수 있는 API의 토큰을 검사한다.
 * 토큰이 설정되지 않은 환경에서는 모든 요청을 거부한다(프로덕션은 docker-compose-prod.yml에서 :?required로 강제).
 */
@Component
public class AgentTokenInterceptor implements HandlerInterceptor {

    static final String AGENT_TOKEN_HEADER = "X-Agent-Token";

    private final byte[] agentToken;

    public AgentTokenInterceptor(@Value("${print-agent.token}") String agentToken) {
        this.agentToken = agentToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // CORS preflight처럼 컨트롤러로 가지 않는 요청은 검사하지 않는다.
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String requestToken = request.getHeader(AGENT_TOKEN_HEADER);
        if (agentToken.length == 0 || requestToken == null
                || !MessageDigest.isEqual(agentToken, requestToken.getBytes(StandardCharsets.UTF_8))) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }
        return true;
    }
}
