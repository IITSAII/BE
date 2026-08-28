package com.iitsaii.photobooth.domain.payment.service;

import com.iitsaii.photobooth.domain.payment.client.TossPaymentClient;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.entity.Payment;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.domain.payment.repository.PaymentRepository;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStatus;
import com.iitsaii.photobooth.domain.session.entity.SessionStep;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    /**
     * 관계 선택(RELATIONSHIP) 단계 타임아웃. CAPTURE 단계와 마찬가지로 잠정치이며,
     * 프론트 UX 확정 후 조정이 필요하다.
     */
    private static final Duration RELATIONSHIP_STEP_TIMEOUT = Duration.ofSeconds(60);

    private final SessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 승인. orderId는 별도 발급하지 않고 Session.sessionId를 그대로 사용한다.
     * 승인 성공 시 Payment row를 생성하고, 세션을 PAID 상태로 전환하며 RELATIONSHIP 단계로 진입시킨다.
     *
     * 같은 세션으로 중복 호출(네트워크 재시도, 중복 클릭 등)되면 재승인을 시도하지 않고
     * 이미 처리된 결과를 그대로 반환한다 (멱등 처리).
     */
    @Transactional
    public SessionStatusResponse confirm(String sessionId, String paymentKey, Integer amount) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        session.expireIfPaymentTimedOut(LocalDateTime.now());
        if (session.getStatus() == SessionStatus.EXPIRED) {
            throw new CustomException(SessionErrorCode.SESSION_EXPIRED);
        }

        Optional<Payment> existingPayment = paymentRepository.findByOrderId(sessionId);
        if (existingPayment.isPresent()) {
            return SessionStatusResponse.from(session);
        }

        if (session.getCurrentStep() != SessionStep.PAYMENT) {
            throw new CustomException(SessionErrorCode.INVALID_STEP);
        }
        if (!session.getAmount().equals(amount)) {
            throw new CustomException(PaymentErrorCode.AMOUNT_MISMATCH);
        }

        TossConfirmResponse response = tossPaymentClient.confirm(
                new TossConfirmRequest(paymentKey, sessionId, amount.longValue()));

        if (response.approvedAt() == null) {
            // 토스 문서상 approvedAt은 nullable이지만, 승인 성공(status=DONE) 응답이라면 항상 채워져야 한다.
            // 여기 걸리면 토스 쪽 응답 이상이므로 방어적으로 막는다.
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED,
                    "토스 응답에 approvedAt이 없습니다.");
        }

        Payment payment = Payment.approved(
                session.getId(),
                sessionId,
                response.paymentKey(),
                response.method(),
                response.approvedAt().toLocalDateTime()
        );

        try {
            paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 이미 다른 트랜잭션이 먼저 처리한 경우.
            // PostgreSQL은 이 시점에 현재 트랜잭션이 abort 상태가 되어 같은 트랜잭션 안에서는
            // 추가 조회(SELECT 포함)도 실패하므로, 여기서 재조회하지 않고 예외를 던져 트랜잭션을 종료한다.
            // 클라이언트는 이 응답을 받고 짧게 재시도하면 된다 (그때는 findByOrderId가 먼저 걸러줌).
            throw new CustomException(SessionErrorCode.CONCURRENT_REQUEST);
        }

        session.completePayment(LocalDateTime.now().plus(RELATIONSHIP_STEP_TIMEOUT));

        return SessionStatusResponse.from(session);
    }
}
