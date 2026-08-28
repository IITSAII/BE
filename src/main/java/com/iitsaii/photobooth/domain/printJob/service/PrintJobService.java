package com.iitsaii.photobooth.domain.printJob.service;

import com.iitsaii.photobooth.domain.printJob.converter.PrintJobConverter;
import com.iitsaii.photobooth.domain.printJob.dto.PrintJobReqDTO;
import com.iitsaii.photobooth.domain.printJob.dto.PrintJobResDTO;
import com.iitsaii.photobooth.domain.printJob.entity.PrintJob;
import com.iitsaii.photobooth.domain.printJob.error.PrintJobErrorCode;
import com.iitsaii.photobooth.domain.printJob.repository.PrintJobRepository;
import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.domain.session.entity.SessionStep;
import com.iitsaii.photobooth.domain.session.error.SessionErrorCode;
import com.iitsaii.photobooth.domain.session.repository.SessionRepository;
import com.iitsaii.photobooth.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PrintJobService {

    private static final Duration PRINT_STEP_TIMEOUT = Duration.ofSeconds(100);

    private final SessionRepository sessionRepository;
    private final PrintJobRepository printJobRepository;

    @Transactional
    public PrintJobResDTO.FrameSelect selectFrame(String sessionId, PrintJobReqDTO.FrameSelect dto) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (session.getCurrentStep() != SessionStep.FRAME) {
            throw new CustomException(PrintJobErrorCode.INVALID_FRAME_STEP);
        }

        if (printJobRepository.existsBySession(session)) {
            throw new CustomException(PrintJobErrorCode.FRAME_ALREADY_SELECTED);
        }

        PrintJob printJob = PrintJob.of(session, dto.frameType(), dto.filterBw(), dto.filterBrightness());
        printJobRepository.save(printJob);

        session.advanceTo(SessionStep.PRINT, LocalDateTime.now().plus(PRINT_STEP_TIMEOUT));

        return PrintJobConverter.toFrameSelect(printJob);
    }
}
