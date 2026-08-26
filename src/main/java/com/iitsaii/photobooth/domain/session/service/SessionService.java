package com.iitsaii.photobooth.domain.session.service;

import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.domain.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.entity.RelationshipType;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStep;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
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

    /**
     * 촬영 단계(CAPTURE) 타임아웃. 컷당 10초 × 6컷 기준의 잠정치이며,
     * 컷 사이 결과 확인 대기시간은 반영되어 있지 않다. photo 도메인 설계 확정 후 조정이 필요하다.
     */
    private static final Duration CAPTURE_STEP_TIMEOUT = Duration.ofSeconds(60);

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

    @Transactional
    public SessionStatusResponse chooseRelationship(String sessionId, RelationshipType relationshipType) {
        try {
            Session session = findBySessionId(sessionId);
            if (session.getCurrentStep() != SessionStep.RELATIONSHIP) {
                throw new CustomException(SessionErrorCode.INVALID_STEP);
            }

            session.chooseRelationship(relationshipType, LocalDateTime.now().plus(CAPTURE_STEP_TIMEOUT));
            sessionRepository.flush();
            return SessionStatusResponse.from(session);
        } catch (OptimisticLockingFailureException e) {
            // 동시 요청으로 같은 세션의 단계를 동시에 전이시키려 한 경우. 먼저 커밋된 요청만 반영한다.
            throw new CustomException(SessionErrorCode.CONCURRENT_REQUEST);
        }
    }

    private Session findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
    }

    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
