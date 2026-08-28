package com.iitsaii.photobooth.global.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile image, String sessionId, Integer shotNumber) {
        String key = createFileKey(sessionId, shotNumber, image);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(image.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패");
        }

        return createFileUrl(key);
    }

    private String createFileKey(String sessionId, Integer shotNumber, MultipartFile image) {

        String extension = getExtension(image.getOriginalFilename());

        return "sessions/%s/shot-%d-%s.%s".formatted(sessionId, shotNumber, UUID.randomUUID(), extension);
    }

    private String createFileUrl(String key) {
        return "https://%s.s3.ap-northeast-2.amazonaws.com/%s"
                .formatted(bucket, key);
    }

    private String getExtension(String filename) {

        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }

        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
