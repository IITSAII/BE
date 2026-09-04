package com.iitsaii.photobooth.domain.session.service;

import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.domain.partner.dto.PartnerResponse;
import com.iitsaii.photobooth.domain.partner.service.PartnerService;
import com.iitsaii.photobooth.domain.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.entity.RelationshipType;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStatus;
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

    /**
     * 결제(PAYMENT) 단계 타임아웃. 결제창 진입부터 승인 완료까지 걸리는 시간을 고려한 잠정치이며,
     * 실사용 데이터 확인 후 조정이 필요하다. 이 시간이 지나면 세션은 EXPIRED로 종료되고 재시작해야 한다.
     */
    private static final Duration PAYMENT_STEP_TIMEOUT = Duration.ofSeconds(60);

    private final SessionRepository sessionRepository;
    private final PartnerService partnerService;

    @Transactional
    public SessionCreateResponse createSession(Integer quantity) {
        Integer amount = QUANTITY_PRICE_TABLE.get(quantity);
        if (amount == null) {
            throw new CustomException(SessionErrorCode.INVALID_QUANTITY);
        }

        String sessionId = generateSessionId();
        Session session = Session.of(sessionId, quantity, amount);
        session.advanceTo(SessionStep.PAYMENT, LocalDateTime.now().plus(PAYMENT_STEP_TIMEOUT));
        sessionRepository.save(session);

        return SessionCreateResponse.from(session);
    }

    @Transactional
    public SessionStatusResponse getStatus(String sessionId) {
        Session session = findBySessionId(sessionId);
        session.expireIfPaymentTimedOut(LocalDateTime.now());

        // 결제 승인 시점(PaymentService.confirm)에 업체 배정이 실패했던 세션을 위한 재시도.
        // 결제는 완료됐는데(PAID) 아직 업체가 없으면 조회 시점마다 한 번씩 다시 배정을 시도한다.
        if (session.getStatus() == SessionStatus.PAID && session.getPartnerId() == null) {
            partnerService.assignPartnerToSession(session);
        }

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

    @Transactional
    public PartnerResponse getAssignedPartner(String sessionId) {
        Session session = findBySessionId(sessionId);
        if (session.getPartnerId() == null) {
            throw new CustomException(SessionErrorCode.PARTNER_NOT_ASSIGNED);
        }

        return PartnerResponse.of(partnerService.getById(session.getPartnerId()), session);
    }

    private Session findBySessionId(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
    }

    private String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
