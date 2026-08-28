package com.iitsaii.photobooth.domain.payment.service;

import com.iitsaii.photobooth.domain.payment.client.TossPaymentClient;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.entity.Payment;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.domain.payment.repository.PaymentRepository;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.entity.Session;
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
            // 동시 요청으로 이미 다른 트랜잭션이 먼저 처리한 경우. 결제는 이미 승인됐으니 최신 세션 상태를 반환한다.
            return SessionStatusResponse.from(
                    sessionRepository.findBySessionId(sessionId)
                            .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND))
            );
        }

        session.completePayment(LocalDateTime.now().plus(RELATIONSHIP_STEP_TIMEOUT));

        return SessionStatusResponse.from(session);
    }
}
