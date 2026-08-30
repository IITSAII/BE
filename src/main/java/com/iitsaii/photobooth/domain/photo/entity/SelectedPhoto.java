package com.iitsaii.photobooth.domain.photo.entity;

import com.iitsaii.photobooth.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/** 6장 중 선택된 사진 (최종 인화 대상, 최대 4장). */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "selected_photos")
public class SelectedPhoto extends BaseEntity {

    /** 선택된 원본 사진 (captured_photos.id 참조) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captured_photo_id", nullable = false)
    private CapturedPhoto capturedPhoto;

    /** 선택 순서이자 인화 배치 순서 (1~4) */
    @Column(name = "select_order", nullable = false)
    private Integer selectOrder;

    public static SelectedPhoto of(CapturedPhoto capturedPhoto, Integer selectOrder) {
        SelectedPhoto selectedPhoto = new SelectedPhoto();
        selectedPhoto.capturedPhoto = capturedPhoto;
        selectedPhoto.selectOrder = selectOrder;
        return selectedPhoto;
    }
}
