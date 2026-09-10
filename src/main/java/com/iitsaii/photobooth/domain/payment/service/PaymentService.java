package com.iitsaii.photobooth.domain.payment.service;

import com.iitsaii.photobooth.domain.payment.client.TossPaymentClient;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmRequest;
import com.iitsaii.photobooth.domain.payment.dto.TossConfirmResponse;
import com.iitsaii.photobooth.domain.payment.entity.Payment;
import com.iitsaii.photobooth.domain.payment.error.PaymentErrorCode;
import com.iitsaii.photobooth.domain.payment.repository.PaymentRepository;
import com.iitsaii.photobooth.domain.partner.service.PartnerService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    /**
     * 관계 선택(RELATIONSHIP) 단계 타임아웃. CAPTURE 단계와 마찬가지로 잠정치이며,
     * 프론트 UX 확정 후 조정이 필요하다.
     */
    private static final Duration RELATIONSHIP_STEP_TIMEOUT = Duration.ofSeconds(60);

    /** 결제 승인 왕복 중 세션이 만료되어 자동 취소할 때 토스에 전달하는 사유. */
    private static final String EXPIRED_SESSION_CANCEL_REASON = "결제 승인 처리 중 세션 만료로 인한 자동 취소";

    private final SessionRepository sessionRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PartnerService partnerService;

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

        TossConfirmResponse response = confirmWithReconciliation(sessionId, paymentKey, amount);

        // status가 DONE이 아니면 승인 완료로 취급하지 않는다.
        // 가상계좌는 입금 전까지 WAITING_FOR_DEPOSIT으로 응답하며(approvedAt=null), 이 서비스는
        // 즉시 확정 결제(카드/간편결제 등)만 지원하므로 DONE이 아닌 응답은 실패로 처리한다.
        if (!"DONE".equals(response.status())) {
            throw new CustomException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED,
                    "토스 결제가 아직 완료되지 않았습니다. status=" + response.status());
        }
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

        // 토스 승인 왕복(타임아웃/재조회 포함, 최대 수십 초) 동안 세션이 만료됐을 수 있다.
        // 이 좁은 구간(수십 초)에는 사용자가 아직 그 자리에 있으므로(환불 정책상 "촬영이 진행되지
        // 않은" 케이스), 뒤늦게 찾아올 사용자를 걱정할 필요 없이 즉시 자동 취소해도 안전하다.
        LocalDateTime now = LocalDateTime.now();
        session.expireIfPaymentTimedOut(now);
        if (session.getStatus() == SessionStatus.EXPIRED) {
            cancelExpiredPayment(payment, sessionId);
            return SessionStatusResponse.from(session);
        }

        session.completePayment(now.plus(RELATIONSHIP_STEP_TIMEOUT));

        // 실패해도 결제는 이미 외부(토스)에서 승인되어 되돌릴 수 없으므로 이 트랜잭션을 막지 않는다.
        // 이후 세션 조회 시점(SessionService.getStatus)에 배정을 다시 시도한다.
        partnerService.assignPartnerToSession(session);

        return SessionStatusResponse.from(session);
    }

    /**
     * 결제 승인 왕복 도중 세션이 만료됐을 때 즉시 자동 환불(취소) 처리한다.
     * 토스 취소 자체가 실패하면(네트워크/토스 장애) 사용자 응답(세션 EXPIRED)은 그대로 두되,
     * Payment 상태는 DONE으로 남겨 운영팀이 로그를 보고 수동으로 재처리할 수 있게 한다 -
     * 실제로 취소되지 않았는데 우리 쪽 기록만 CANCELED로 바꾸면 안 되기 때문이다.
     */
    private void cancelExpiredPayment(Payment payment, String sessionId) {
        try {
            tossPaymentClient.cancel(payment.getPaymentKey(), EXPIRED_SESSION_CANCEL_REASON);
            payment.cancel();
        } catch (CustomException e) {
            log.error("결제 승인 후 세션 만료로 자동 취소를 시도했으나 실패했습니다. "
                    + "수동 환불 검토 필요. sessionId={}, paymentKey={}", sessionId, payment.getPaymentKey(), e);
        }
    }

    /**
     * 토스 승인을 요청하고, 애매한 실패(타임아웃/네트워크 오류/5xx)면 orderId로 재조회해서
     * 실제로는 승인이 됐는지 한 번 더 확인한다 ("서버는 성공, 응답만 유실"된 상황 방어).
     * 토스가 4xx로 명확히 거부한 경우는 재확인 없이 그대로 실패 처리한다.
     */
    private TossConfirmResponse confirmWithReconciliation(String sessionId, String paymentKey, Integer amount) {
        try {
            return tossPaymentClient.confirm(new TossConfirmRequest(paymentKey, sessionId, amount.longValue()));
        } catch (CustomException e) {
            if (e.getErrorCode() != PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE) {
                throw e;
            }
            return tossPaymentClient.findApprovedByOrderId(sessionId).orElseThrow(() -> e);
        } catch (RestClientException e) {
            return tossPaymentClient.findApprovedByOrderId(sessionId)
                    .orElseThrow(() -> new CustomException(PaymentErrorCode.PAYMENT_GATEWAY_UNAVAILABLE,
                            "결제 서비스 응답을 받지 못했습니다."));
        }
    }
}
