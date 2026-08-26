package com.iitsaii.photobooth.photo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** iPad에서 촬영된 원본 사진 (세션당 최대 6장). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "captured_photos")
public class CapturedPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 연결된 세션 (sessions.id 참조) */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 촬영 순서 (1~6번째 컷) */
    @Column(name = "shot_number", nullable = false)
    private Integer shotNumber;

    /** 원본 이미지 저장 경로 (S3) */
    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static CapturedPhoto of(Long sessionId, Integer shotNumber, String imageUrl) {
        CapturedPhoto capturedPhoto = new CapturedPhoto();
        capturedPhoto.sessionId = sessionId;
        capturedPhoto.shotNumber = shotNumber;
        capturedPhoto.imageUrl = imageUrl;
        return capturedPhoto;
    }
}
