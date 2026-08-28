package com.iitsaii.photobooth.domain.photo.service;

import com.iitsaii.photobooth.domain.photo.converter.PhotoConverter;
import com.iitsaii.photobooth.domain.photo.dto.PhotoReqDTO;
import com.iitsaii.photobooth.domain.photo.dto.PhotoResDTO;
import com.iitsaii.photobooth.domain.photo.entity.CapturedPhoto;
import com.iitsaii.photobooth.domain.photo.entity.SelectedPhoto;
import com.iitsaii.photobooth.domain.photo.error.PhotoErrorCode;
import com.iitsaii.photobooth.domain.photo.repository.CapturedPhotoRepository;
import com.iitsaii.photobooth.domain.photo.repository.SelectedPhotoRepository;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final int MAX_SHOT_COUNT = 6;
    private static final Duration SELECT_STEP_TIMEOUT = Duration.ofSeconds(100);
    private static final Duration FRAME_STEP_TIMEOUT = Duration.ofSeconds(100);

    private final SessionRepository sessionRepository;
    private final CapturedPhotoRepository capturedPhotoRepository;
    private final SelectedPhotoRepository selectedPhotoRepository;
    private final S3Service s3Service;

    @Transactional
    public PhotoResDTO.SavePhoto savePhoto(String sessionId, Integer shotNumber, MultipartFile image) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (session.getCurrentStep() != SessionStep.CAPTURE) {
            throw new CustomException(PhotoErrorCode.INVALID_CAPTURE_STEP);
        }

        if (capturedPhotoRepository.countBySession(session) >= MAX_SHOT_COUNT) {
            throw new CustomException(PhotoErrorCode.PHOTO_LIMIT_EXCEEDED);
        }

        if (shotNumber == null || shotNumber < 1 || shotNumber > MAX_SHOT_COUNT) {
            throw new CustomException(PhotoErrorCode.INVALID_SHOT_NUMBER);
        }

        if (image == null || image.isEmpty()) {
            throw new CustomException(PhotoErrorCode.EMPTY_IMAGE);
        }

        String imageUrl = s3Service.upload(image, session.getSessionId(), shotNumber);
        CapturedPhoto capturedPhoto;

        try {
            capturedPhoto = CapturedPhoto.of(session, shotNumber, imageUrl);
            capturedPhotoRepository.saveAndFlush(capturedPhoto);
        } catch (DataIntegrityViolationException e) {
            s3Service.delete(imageUrl);
            throw new CustomException(PhotoErrorCode.PHOTO_ALREADY_EXISTS);
        }

        if (capturedPhotoRepository.countBySession(session) == MAX_SHOT_COUNT) {
            session.advanceTo(SessionStep.SELECT, LocalDateTime.now().plus(SELECT_STEP_TIMEOUT));
        }

        return PhotoConverter.toSavePhoto(capturedPhoto);
    }

    @Transactional(readOnly = true)
    public PhotoResDTO.PhotoList getPhotos(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        List<CapturedPhoto> photos = capturedPhotoRepository.findBySessionOrderByShotNumber(session);
        return PhotoConverter.toPhotoList(photos);
    }

    @Transactional
    public void selectPhotos(String sessionId, PhotoReqDTO.SelectPhotos dto) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));

        if (session.getCurrentStep() != SessionStep.SELECT) {
            throw new CustomException(PhotoErrorCode.INVALID_SELECT_STEP);
        }

        List<Long> photoIds = dto.photoIds();

        if (photoIds == null || photoIds.size() != 4) {
            throw new CustomException(PhotoErrorCode.INVALID_SELECTED_COUNT);
        }

        if (new HashSet<>(photoIds).size() != 4) {
            throw new CustomException(PhotoErrorCode.DUPLICATE_SELECTED_PHOTO);
        }

        if (selectedPhotoRepository.existsByCapturedPhoto_Session(session)) {
            throw new CustomException(PhotoErrorCode.PHOTOS_ALREADY_SELECTED);
        }

        List<CapturedPhoto> photos = capturedPhotoRepository.findAllBySessionAndIdIn(session, photoIds);

        if (photos.size() != 4) {
            throw new CustomException(PhotoErrorCode.INVALID_SELECTED_PHOTO);
        }

        Map<Long, CapturedPhoto> photoMap = photos.stream().collect(Collectors.toMap(CapturedPhoto::getId, Function.identity()));
        List<SelectedPhoto> selectedPhotos = new ArrayList<>();

        for (int i = 0; i < photoIds.size(); i++) {
            Long photoId = photoIds.get(i);
            CapturedPhoto photo = photoMap.get(photoId);
            selectedPhotos.add(SelectedPhoto.of(photo, i + 1));
        }

        selectedPhotoRepository.saveAll(selectedPhotos);
        session.advanceTo(SessionStep.FRAME, LocalDateTime.now().plus(FRAME_STEP_TIMEOUT));
    }

    @Transactional(readOnly = true)
    public PhotoResDTO.SelectedPhotoList getSelectedPhotos(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow(() -> new CustomException(SessionErrorCode.SESSION_NOT_FOUND));
        List<SelectedPhoto> selectedPhotos = selectedPhotoRepository.findAllByCapturedPhoto_SessionOrderBySelectOrderAsc(session);
        return PhotoConverter.toSelectedPhotoList(selectedPhotos);
    }
}
