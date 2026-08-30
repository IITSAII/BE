package com.iitsaii.photobooth.domain.photo.entity;

import com.iitsaii.photobooth.domain.session.entity.Session;
import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** iPad에서 촬영된 원본 사진 (세션당 최대 6장). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "captured_photos",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_shot",
                        columnNames = {"session_id", "shot_number"}
                )
        }
)
public class CapturedPhoto extends BaseEntity {

    /** 연결된 세션 (sessions.id 참조) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    /** 촬영 순서 (1~6번째 컷) */
    @Column(name = "shot_number", nullable = false)
    private Integer shotNumber;

    /** 원본 이미지 저장 경로 (S3) */
    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    public static CapturedPhoto of(Session session, Integer shotNumber, String imageUrl) {
        CapturedPhoto capturedPhoto = new CapturedPhoto();
        capturedPhoto.session = session;
        capturedPhoto.shotNumber = shotNumber;
        capturedPhoto.imageUrl = imageUrl;
        return capturedPhoto;
    }
}
