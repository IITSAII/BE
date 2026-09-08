package com.iitsaii.photobooth.domain.printJob.service;

import com.iitsaii.photobooth.domain.photo.error.PhotoErrorCode;
import com.iitsaii.photobooth.domain.printJob.converter.PrintJobConverter;
import com.iitsaii.photobooth.domain.printJob.dto.PrintJobReqDTO;
import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJobStatus;
import com.iitsaii.photobooth.domain.printJob.error.PrintJobErrorCode;
import com.iitsaii.photobooth.domain.printJob.repository.PrintJobRepository;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStep;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import com.iitsaii.photobooth.global.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrintJobService {

    private static final Duration PRINT_STEP_TIMEOUT = Duration.ofSeconds(100);

    /** sessionId 기반 조회(GET .../print)의 열람 가능 기한. 이 기간이 지나도 galleryToken으로는 계속 접근 가능하다. */
    private static final Duration PHOTO_VIEW_WINDOW = Duration.ofHours(24);

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png");

    private final SessionRepository sessionRepository;
    private final PrintJobRepository printJobRepository;
    private final S3Service s3Service;

    @Transactional
    public PrintJobResDTO.FrameSelect selectFrame(String sessionId, PrintJobReqDTO.FrameSelect dto) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (session.getCurrentStep() != SessionStep.FRAME) {
            throw new CustomException(PrintJobErrorCode.INVALID_FRAME_STEP);
        }

        PrintJob printJob = PrintJob.of(session, dto.frameType(), dto.filterBw(), dto.filterBrightness());

        try {
            printJobRepository.saveAndFlush(printJob);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(PrintJobErrorCode.FRAME_ALREADY_SELECTED);
        }

        return PrintJobConverter.toFrameSelect(printJob);
    }

    @Transactional
    public PrintJobResDTO.UploadFinalImage uploadFinalImage(String sessionId, MultipartFile finalImage) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (session.getCurrentStep() != SessionStep.FRAME) {
            throw new CustomException(PrintJobErrorCode.INVALID_FRAME_STEP);
        }

        PrintJob printJob = printJobRepository.findBySession(session).orElseThrow(() -> new CustomException(PrintJobErrorCode.PRINT_JOB_NOT_FOUND));

        if (finalImage == null || finalImage.isEmpty()) {
            throw new CustomException(PhotoErrorCode.EMPTY_IMAGE);
        }

        if (!ALLOWED_IMAGE_TYPES.contains(finalImage.getContentType())) {
            throw new CustomException(PrintJobErrorCode.INVALID_IMAGE_FILE);
        }

        try (InputStream inputStream = finalImage.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);

            if (image == null) {
                throw new CustomException(PrintJobErrorCode.INVALID_IMAGE_FILE);
            }
        } catch (IOException e) {
            throw new CustomException(PrintJobErrorCode.INVALID_IMAGE_FILE);
        }

        String imageUrl = s3Service.uploadFinalImage(finalImage, session.getSessionId());

        printJob.updateFinalImage(imageUrl);

        LocalDateTime now = LocalDateTime.now();
        session.advanceTo(SessionStep.PRINT, now.plus(PRINT_STEP_TIMEOUT));
        session.startPhotoViewWindow(now.plus(PHOTO_VIEW_WINDOW));

        return PrintJobConverter.toUploadFinalImage(printJob);
    }

    /**
     * sessionId로 최종 인쇄 이미지를 조회한다 (촬영 직후 화면용).
     * PHOTO_VIEW_WINDOW는 galleryToken으로 접근하든 여기로 접근하든 동일하게 적용된다 -
     * 만료 없이 접근 가능한 건 갤러리(매거진/쿠폰) 페이지 자체이지, 사진 열람은 아니다.
     */
    @Transactional(readOnly = true)
    public PrintJobResDTO.PrintInfo getPrintInfo(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        return getPrintInfo(session);
    }

    /**
     * galleryToken(인화물 QR/바코드)으로 최종 인쇄 이미지를 조회한다.
     * 토큰 자체(갤러리 접속)는 만료되지 않지만, 사진 열람은 sessionId 접근과 동일하게
     * PHOTO_VIEW_WINDOW가 지나면 더 이상 조회할 수 없다.
     */
    @Transactional(readOnly = true)
    public PrintJobResDTO.PrintInfo getPrintInfoByGalleryToken(UUID galleryToken) {
        Session session = sessionRepository.findByGalleryToken(galleryToken)
                .orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        return getPrintInfo(session);
    }

    private PrintJobResDTO.PrintInfo getPrintInfo(Session session) {
        PrintJob printJob = printJobRepository.findBySession(session).orElseThrow(() -> new CustomException(PrintJobErrorCode.PRINT_JOB_NOT_FOUND));

        if (printJob.getFinalImageUrl() == null) {
            throw new CustomException(PrintJobErrorCode.FINAL_IMAGE_NOT_READY);
        }

        if (session.isPhotoViewExpired(LocalDateTime.now())) {
            throw new CustomException(PrintJobErrorCode.PHOTO_VIEW_EXPIRED);
        }

        return PrintJobConverter.toPrintInfo(printJob);
    }

    @Transactional
    public void completePrint(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        PrintJob printJob = printJobRepository.findBySessionForUpdate(session).orElseThrow(() -> new CustomException(PrintJobErrorCode.PRINT_JOB_NOT_FOUND));

        if (printJob.getStatus() == PrintJobStatus.DONE) {
            throw new CustomException(PrintJobErrorCode.PRINT_ALREADY_DONE);
        }

        if (printJob.getFinalImageUrl() == null) {
            throw new CustomException(PrintJobErrorCode.FINAL_IMAGE_NOT_READY);
        }

        if (printJob.getStatus() != PrintJobStatus.PRINTING) {
            throw new CustomException(PrintJobErrorCode.INVALID_PRINT_STATUS);
        }

        printJob.markDone();
        session.advanceTo(SessionStep.DONE, null);
    }

    @Transactional
    public void failPrint(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        PrintJob printJob = printJobRepository.findBySessionForUpdate(session).orElseThrow(() -> new CustomException(PrintJobErrorCode.PRINT_JOB_NOT_FOUND));

        if (printJob.getStatus() == PrintJobStatus.DONE) {
            throw new CustomException(PrintJobErrorCode.PRINT_ALREADY_DONE);
        }

        if (printJob.getFinalImageUrl() == null) {
            throw new CustomException(PrintJobErrorCode.FINAL_IMAGE_NOT_READY);
        }

        if (printJob.getStatus() != PrintJobStatus.PRINTING) {
            throw new CustomException(PrintJobErrorCode.INVALID_PRINT_STATUS);
        }

        printJob.markFailed();
    }

    @Transactional
    public PrintJobResDTO.PrintQueue getPrintQueue() {
        PrintJob printJob = printJobRepository.findFirstByStatusAndFinalImageUrlIsNotNullOrderByCreatedAtAsc(PrintJobStatus.QUEUED).orElseThrow(() -> new CustomException(PrintJobErrorCode.NO_PRINT_JOB_IN_QUEUE));

        printJob.markPrinting();

        return PrintJobConverter.toPrintQueue(printJob);
    }
}
