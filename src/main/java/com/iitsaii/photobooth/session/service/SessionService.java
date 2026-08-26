package com.iitsaii.photobooth.session.service;

import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.session.entity.Session;
import com.iitsaii.photobooth.session.error.SessionErrorCode;
import com.iitsaii.photobooth.session.repository.SessionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    /** 수량별 단가 정책. 이 세 값 외의 수량은 허용하지 않는다. */
    private static final Map<Integer, Integer> QUANTITY_PRICE_TABLE = Map.of(
            2, 3000,
            4, 6000,
            6, 9000
    );

    private final SessionRepository sessionRepository;

    @Transactional
    public SessionCreateResponse createSession(Integer quantity) {
        Integer amount = QUANTITY_PRICE_TABLE.get(quantity);
        if (amount == null) {
            throw new CustomException(SessionErrorCode.INVALID_QUANTITY);
        }

        String sessionId = generateSessionId();
        Session session = Session.of(sessionId, quantity, amount);
        sessionRepository.save(session);

        return SessionCreateResponse.from(session);
    }

    public SessionStatusResponse getStatus(String sessionId) {
        Session session = findBySessionId(sessionId);
        return SessionStatusResponse.from(session);
    }

    private Session findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
    }

    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
