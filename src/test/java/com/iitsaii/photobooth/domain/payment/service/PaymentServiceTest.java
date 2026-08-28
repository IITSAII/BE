package com.iitsaii.photobooth.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.iitsaii.photobooth.domain.payment.client.TossPaymentClient;
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
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @InjectMocks
    private PaymentService paymentService;

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("PAYMENT 단계이고 금액이 일치하면 승인 후 세션을 PAID/RELATIONSHIP으로 전이한다")
        void confirmsPaymentAndAdvancesSession() {
            Session session = Session.of("sess_abc123", 4, 6000);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));
            given(tossPaymentClient.confirm(any())).willReturn(
                    new TossConfirmResponse("pay_key_1", "sess_abc123", "DONE", "카드",
                            OffsetDateTime.now(), 6000L)
            );

            SessionStatusResponse response = paymentService.confirm("sess_abc123", "pay_key_1", 6000);

            assertThat(response.status()).isEqualTo(SessionStatus.PAID.name());
            assertThat(response.currentStep()).isEqualTo(SessionStep.RELATIONSHIP.name());
            assertThat(session.getStepExpiresAt()).isNotNull();
            verify(paymentRepository).saveAndFlush(any(Payment.class));
        }

        @Test
        @DisplayName("존재하지 않는 세션이면 SESSION_NOT_FOUND 예외를 던진다")
        void throwsWhenSessionNotFound() {
            given(sessionRepository.findBySessionId("sess_none")).willReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.confirm("sess_none", "pay_key_1", 6000))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);

            verify(tossPaymentClient, never()).confirm(any());
        }

        @Test
        @DisplayName("PAYMENT 단계가 아니면 INVALID_STEP 예외를 던진다")
        void throwsWhenNotInPaymentStep() {
            Session session = Session.of("sess_abc123", 4, 6000);
            session.advanceTo(SessionStep.RELATIONSHIP, null);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            assertThatThrownBy(() -> paymentService.confirm("sess_abc123", "pay_key_1", 6000))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.INVALID_STEP);

            verify(tossPaymentClient, never()).confirm(any());
        }

        @Test
        @DisplayName("요청 금액이 세션 금액과 다르면 AMOUNT_MISMATCH 예외를 던지고 토스를 호출하지 않는다")
        void throwsWhenAmountMismatch() {
            Session session = Session.of("sess_abc123", 4, 6000);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            assertThatThrownBy(() -> paymentService.confirm("sess_abc123", "pay_key_1", 9999))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.AMOUNT_MISMATCH);

            verify(tossPaymentClient, never()).confirm(any());
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("토스 승인이 실패하면 예외가 그대로 전파되고 세션/결제 상태는 바뀌지 않는다")
        void propagatesTossFailureWithoutChangingState() {
            Session session = Session.of("sess_abc123", 4, 6000);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));
            given(tossPaymentClient.confirm(any()))
                    .willThrow(new CustomException(PaymentErrorCode.PAYMENT_CONFIRM_FAILED));

            assertThatThrownBy(() -> paymentService.confirm("sess_abc123", "pay_key_1", 6000))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(PaymentErrorCode.PAYMENT_CONFIRM_FAILED);

            assertThat(session.getCurrentStep()).isEqualTo(SessionStep.PAYMENT);
            verify(paymentRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("이미 처리된 결제(orderId로 조회됨)면 재승인 없이 현재 세션 상태를 그대로 반환한다")
        void returnsExistingResultWhenAlreadyConfirmed() {
            Session session = Session.of("sess_abc123", 4, 6000);
            session.completePayment(null);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));
            given(paymentRepository.findByOrderId("sess_abc123")).willReturn(Optional.of(
                    Payment.approved(1L, "sess_abc123", "pay_key_1", "카드", java.time.LocalDateTime.now())
            ));

            SessionStatusResponse response = paymentService.confirm("sess_abc123", "pay_key_1", 6000);

            assertThat(response.status()).isEqualTo(SessionStatus.PAID.name());
            assertThat(response.currentStep()).isEqualTo(SessionStep.RELATIONSHIP.name());
            verify(tossPaymentClient, never()).confirm(any());
            verify(paymentRepository, never()).saveAndFlush(any());
        }
    }
}
