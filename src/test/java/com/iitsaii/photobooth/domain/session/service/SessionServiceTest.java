package com.iitsaii.photobooth.domain.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.domain.session.dto.SessionCreateResponse;
import com.iitsaii.photobooth.domain.session.dto.SessionStatusResponse;
import com.iitsaii.photobooth.domain.session.entity.RelationshipType;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStatus;
import com.iitsaii.photobooth.domain.session.entity.SessionStep;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionService sessionService;

    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @ParameterizedTest
        @DisplayName("허용된 수량이면 단가 정책에 따라 금액을 계산해 세션을 생성한다")
        @org.junit.jupiter.params.provider.CsvSource({
                "2, 3000",
                "4, 6000",
                "6, 9000",
        })
        void createsSessionWithMappedAmount(int quantity, int expectedAmount) {
            SessionCreateResponse response = sessionService.createSession(quantity);

            assertThat(response.amount()).isEqualTo(expectedAmount);
            assertThat(response.currentStep()).isEqualTo(SessionStep.PAYMENT.name());
            assertThat(response.sessionId()).startsWith("sess_");
            verify(sessionRepository).save(any(Session.class));
        }

        @ParameterizedTest
        @DisplayName("허용되지 않은 수량이면 INVALID_QUANTITY 예외를 던진다")
        @ValueSource(ints = {1, 3, 5, 7, 0, -1})
        void throwsWhenQuantityNotAllowed(int quantity) {
            assertThatThrownBy(() -> sessionService.createSession(quantity))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.INVALID_QUANTITY);
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("존재하는 세션이면 상태를 반환한다")
        void returnsStatusWhenSessionExists() {
            Session session = Session.of("sess_abc123", 4, 6000);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            SessionStatusResponse response = sessionService.getStatus("sess_abc123");

            assertThat(response.sessionId()).isEqualTo("sess_abc123");
            assertThat(response.status()).isEqualTo(SessionStatus.CREATED.name());
            assertThat(response.currentStep()).isEqualTo(SessionStep.PAYMENT.name());
        }

        @Test
        @DisplayName("존재하지 않는 세션이면 SESSION_NOT_FOUND 예외를 던진다")
        void throwsWhenSessionNotFound() {
            given(sessionRepository.findBySessionId("sess_none")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.getStatus("sess_none"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("chooseRelationship")
    class ChooseRelationship {

        @Test
        @DisplayName("RELATIONSHIP 단계이면 관계를 저장하고 CAPTURE 단계로 전이한다")
        void advancesToCaptureWhenInRelationshipStep() {
            Session session = Session.of("sess_abc123", 4, 6000);
            session.advanceTo(SessionStep.RELATIONSHIP, LocalDateTime.now());
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            SessionStatusResponse response = sessionService.chooseRelationship("sess_abc123", RelationshipType.COUPLE);

            assertThat(response.currentStep()).isEqualTo(SessionStep.CAPTURE.name());
            assertThat(session.getRelationshipType()).isEqualTo(RelationshipType.COUPLE);
            assertThat(session.getStepExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("relationshipType이 null이면 '설정 안 함'으로 저장한다")
        void allowsNullRelationshipType() {
            Session session = Session.of("sess_abc123", 4, 6000);
            session.advanceTo(SessionStep.RELATIONSHIP, LocalDateTime.now());
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            sessionService.chooseRelationship("sess_abc123", null);

            assertThat(session.getRelationshipType()).isNull();
            assertThat(session.getCurrentStep()).isEqualTo(SessionStep.CAPTURE);
        }

        @Test
        @DisplayName("RELATIONSHIP 단계가 아니면 INVALID_STEP 예외를 던진다")
        void throwsWhenNotInRelationshipStep() {
            Session session = Session.of("sess_abc123", 4, 6000);
            given(sessionRepository.findBySessionId("sess_abc123")).willReturn(Optional.of(session));

            assertThatThrownBy(() -> sessionService.chooseRelationship("sess_abc123", RelationshipType.FRIEND))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.INVALID_STEP);
        }

        @Test
        @DisplayName("존재하지 않는 세션이면 SESSION_NOT_FOUND 예외를 던진다")
        void throwsWhenSessionNotFound() {
            given(sessionRepository.findBySessionId("sess_none")).willReturn(Optional.empty());

            assertThatThrownBy(() -> sessionService.chooseRelationship("sess_none", RelationshipType.FRIEND))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
        }
    }
}
