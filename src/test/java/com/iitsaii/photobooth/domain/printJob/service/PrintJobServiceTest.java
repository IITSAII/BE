package com.iitsaii.photobooth.domain.printJob.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.entity.FrameType;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.printJob.error.PrintJobErrorCode;
import com.iitsaii.photobooth.domain.printJob.repository.PrintJobRepository;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.global.s3.S3Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PrintJobServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PrintJobRepository printJobRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PrintJobService printJobService;

    private Session sessionWithFinalImage(LocalDateTime photoViewExpiresAt) {
        Session session = Session.of("sess_test", 2, 3000);
        // capturedAt 계산에 쓰이는 createdAt은 영속화(JPA Auditing) 시점에만 채워지므로 직접 세팅한다.
        ReflectionTestUtils.setField(session, "createdAt", LocalDateTime.now());
        if (photoViewExpiresAt != null) {
            session.startPhotoViewWindow(photoViewExpiresAt);
        }
        return session;
    }

    private PrintJob printJobWithFinalImage(Session session) {
        PrintJob printJob = PrintJob.of(session, FrameType.DARK, false, 0);
        printJob.updateFinalImage("https://example.com/final.jpg");
        return printJob;
    }

    @Nested
    @DisplayName("getPrintInfo (sessionId 기반 조회)")
    class GetPrintInfo {

        @Test
        @DisplayName("열람 기한 전이면 정상 조회된다")
        void returnsPrintInfoWhenNotExpiredBySessionId() {
            Session session = sessionWithFinalImage(LocalDateTime.now().plusHours(1));
            PrintJob printJob = printJobWithFinalImage(session);
            given(sessionRepository.findBySessionId("sess_test")).willReturn(Optional.of(session));
            given(printJobRepository.findBySession(session)).willReturn(Optional.of(printJob));

            PrintJobResDTO.PrintInfo result = printJobService.getPrintInfo("sess_test");

            assertThat(result.finalImageUrl()).isEqualTo("https://example.com/final.jpg");
        }

        @Test
        @DisplayName("열람 기한이 지나면 PHOTO_VIEW_EXPIRED 예외를 던진다")
        void throwsWhenPhotoViewExpired() {
            Session session = sessionWithFinalImage(LocalDateTime.now().minusMinutes(1));
            PrintJob printJob = printJobWithFinalImage(session);
            given(sessionRepository.findBySessionId("sess_test")).willReturn(Optional.of(session));
            given(printJobRepository.findBySession(session)).willReturn(Optional.of(printJob));

            assertThatThrownBy(() -> printJobService.getPrintInfo("sess_test"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(PrintJobErrorCode.PHOTO_VIEW_EXPIRED);
        }

        @Test
        @DisplayName("열람 기한이 아직 설정되지 않았으면(null) 만료로 취급하지 않는다")
        void doesNotExpireWhenWindowNotSet() {
            Session session = sessionWithFinalImage(null);
            PrintJob printJob = printJobWithFinalImage(session);
            given(sessionRepository.findBySessionId("sess_test")).willReturn(Optional.of(session));
            given(printJobRepository.findBySession(session)).willReturn(Optional.of(printJob));

            PrintJobResDTO.PrintInfo result = printJobService.getPrintInfo("sess_test");

            assertThat(result.finalImageUrl()).isEqualTo("https://example.com/final.jpg");
        }
    }

    @Nested
    @DisplayName("getPrintInfoByGalleryToken (인화물 QR/바코드 기반 조회)")
    class GetPrintInfoByGalleryToken {

        @Test
        @DisplayName("열람 기한 전이면 galleryToken으로도 정상 조회된다")
        void returnsPrintInfoWhenNotExpired() {
            Session session = sessionWithFinalImage(LocalDateTime.now().plusHours(1));
            PrintJob printJob = printJobWithFinalImage(session);
            UUID galleryToken = session.getGalleryToken();
            given(sessionRepository.findByGalleryToken(galleryToken)).willReturn(Optional.of(session));
            given(printJobRepository.findBySession(session)).willReturn(Optional.of(printJob));

            PrintJobResDTO.PrintInfo result = printJobService.getPrintInfoByGalleryToken(galleryToken);

            assertThat(result.finalImageUrl()).isEqualTo("https://example.com/final.jpg");
        }

        @Test
        @DisplayName("galleryToken 자체는 만료되지 않지만, sessionId 기준 열람 기한이 지났으면 사진 조회는 똑같이 막힌다")
        void throwsWhenPhotoViewExpiredEvenViaGalleryToken() {
            Session session = sessionWithFinalImage(LocalDateTime.now().minusDays(1));
            PrintJob printJob = printJobWithFinalImage(session);
            UUID galleryToken = session.getGalleryToken();
            given(sessionRepository.findByGalleryToken(galleryToken)).willReturn(Optional.of(session));
            given(printJobRepository.findBySession(session)).willReturn(Optional.of(printJob));

            assertThatThrownBy(() -> printJobService.getPrintInfoByGalleryToken(galleryToken))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(PrintJobErrorCode.PHOTO_VIEW_EXPIRED);
        }

        @Test
        @DisplayName("존재하지 않는 galleryToken이면 SESSION_NOT_FOUND 예외를 던진다")
        void throwsWhenGalleryTokenNotFound() {
            UUID unknownToken = UUID.randomUUID();
            given(sessionRepository.findByGalleryToken(unknownToken)).willReturn(Optional.empty());

            assertThatThrownBy(() -> printJobService.getPrintInfoByGalleryToken(unknownToken))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(SessionErrorCode.SESSION_NOT_FOUND);
        }
    }
}
