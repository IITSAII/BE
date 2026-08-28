package com.iitsaii.photobooth.domain.photo.converter;

import com.iitsaii.photobooth.domain.photo.dto.PhotoResDTO;
import com.iitsaii.photobooth.domain.photo.entity.CapturedPhoto;
import com.iitsaii.photobooth.domain.photo.entity.SelectedPhoto;

import java.util.List;

public class PhotoConverter {

    public static PhotoResDTO.SavePhoto toSavePhoto(CapturedPhoto capturedPhoto) {
        return PhotoResDTO.SavePhoto.builder()
                .photoId(capturedPhoto.getId())
                .shotNumber(capturedPhoto.getShotNumber())
                .imageUrl(capturedPhoto.getImageUrl())
                .build();
    }

    public static PhotoResDTO.PhotoInfo toPhotoInfo(CapturedPhoto capturedPhoto) {
        return PhotoResDTO.PhotoInfo.builder()
                .photoId(capturedPhoto.getId())
                .shotNumber(capturedPhoto.getShotNumber())
                .imageUrl(capturedPhoto.getImageUrl())
                .build();
    }

    public static PhotoResDTO.PhotoList toPhotoList(List<CapturedPhoto> photos) {
        return PhotoResDTO.PhotoList.builder()
                .photos(photos.stream().map(PhotoConverter::toPhotoInfo).toList())
                .build();
    }

    public static PhotoResDTO.SelectedPhotoInfo toSelectedPhotoInfo(SelectedPhoto selectedPhoto) {
        return PhotoResDTO.SelectedPhotoInfo.builder()
                .photoId(selectedPhoto.getId())
                .selectOrder(selectedPhoto.getSelectOrder())
                .imageUrl(selectedPhoto.getCapturedPhoto().getImageUrl())
                .build();
    }

    public static PhotoResDTO.SelectedPhotoList toSelectedPhotoList(List<SelectedPhoto> photos) {
        return PhotoResDTO.SelectedPhotoList.builder()
                .photos(photos.stream().map(PhotoConverter::toSelectedPhotoInfo).toList())
                .build();
    }
}
