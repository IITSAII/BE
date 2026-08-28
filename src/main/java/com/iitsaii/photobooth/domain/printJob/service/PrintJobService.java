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

@Service
@RequiredArgsConstructor
public class PrintJobService {

    private static final Duration PRINT_STEP_TIMEOUT = Duration.ofSeconds(100);

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

        session.advanceTo(SessionStep.PRINT, LocalDateTime.now().plus(PRINT_STEP_TIMEOUT));

        return PrintJobConverter.toUploadFinalImage(printJob);
    }

    @Transactional(readOnly = true)
    public PrintJobResDTO.PrintInfo getPrintInfo(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        PrintJob printJob = printJobRepository.findBySession(session).orElseThrow(() -> new CustomException(PrintJobErrorCode.PRINT_JOB_NOT_FOUND));

        if (printJob.getFinalImageUrl() == null) {
            throw new CustomException(PrintJobErrorCode.FINAL_IMAGE_NOT_READY);
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
