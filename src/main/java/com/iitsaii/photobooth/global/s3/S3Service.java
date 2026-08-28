package com.iitsaii.photobooth.global.s3;

import com.iitsaii.photobooth.domain.photo.error.PhotoErrorCode;
import com.iitsaii.photobooth.global.error.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    public String upload(MultipartFile image, String sessionId, Integer shotNumber) {
        String key = createFileKey(sessionId, shotNumber, image);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));

            return createFileUrl(key);
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new CustomException(PhotoErrorCode.PHOTO_UPLOAD_FAILED);
        }
    }

    public void delete(String imageUrl) {
        String key = extractKeyFromUrl(imageUrl);

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception | SdkClientException e) {
            throw new CustomException(PhotoErrorCode.PHOTO_UPLOAD_FAILED);
        }
    }

    private String createFileKey(String sessionId, Integer shotNumber, MultipartFile image) {

        String extension = getExtension(image.getOriginalFilename());

        return "sessions/%s/shot-%d-%s.%s".formatted(sessionId, shotNumber, UUID.randomUUID(), extension);
    }

    private String createFileUrl(String key) {
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(bucket, region, key);
    }

    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }

        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String extractKeyFromUrl(String imageUrl) {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        return imageUrl.replace(prefix, "");
    }
}
