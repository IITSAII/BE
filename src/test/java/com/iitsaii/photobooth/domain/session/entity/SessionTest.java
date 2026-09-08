package com.iitsaii.photobooth.domain.session.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SessionTest {

    @Nested
    @DisplayName("isPhotoViewExpired")
    class IsPhotoViewExpired {

        @Test
        @DisplayName("열람 기한 이전이면 만료가 아니다")
        void notExpiredBeforeDeadline() {
            Session session = Session.of("sess_test", 2, 3000);
            LocalDateTime expiresAt = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
            session.startPhotoViewWindow(expiresAt);

            assertThat(session.isPhotoViewExpired(expiresAt.minusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("열람 기한과 정확히 같은 시각이면 만료로 취급한다 (경계값)")
        void expiredExactlyAtDeadline() {
            Session session = Session.of("sess_test", 2, 3000);
            LocalDateTime expiresAt = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
            session.startPhotoViewWindow(expiresAt);

            assertThat(session.isPhotoViewExpired(expiresAt)).isTrue();
        }

        @Test
        @DisplayName("열람 기한이 지났으면 만료다")
        void expiredAfterDeadline() {
            Session session = Session.of("sess_test", 2, 3000);
            LocalDateTime expiresAt = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
            session.startPhotoViewWindow(expiresAt);

            assertThat(session.isPhotoViewExpired(expiresAt.plusSeconds(1))).isTrue();
        }

        @Test
        @DisplayName("열람 기한이 설정되지 않았으면(null) 만료로 취급하지 않는다")
        void notExpiredWhenNotSet() {
            Session session = Session.of("sess_test", 2, 3000);

            assertThat(session.isPhotoViewExpired(LocalDateTime.now())).isFalse();
        }
    }
}
