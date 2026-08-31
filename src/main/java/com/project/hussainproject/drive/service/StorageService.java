package com.project.hussainproject.drive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Presigner presigner;

    @Value("${supabase.s3.bucket-name}")
    private String bucketName;

    /**
     * Generates a presigned PUT URL so the browser can upload directly to S3.
     * Valid for 15 minutes.
     */
    public String generateUploadUrl(String storageKey, String mimeType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(mimeType)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
        );

        return presignedRequest.url().toString();
    }

    /**
     * Generates a presigned GET URL so the browser can download a file directly from S3.
     * Valid for 60 minutes.
     */
    public String generateDownloadUrl(String storageKey) {
        return generateDownloadUrl(storageKey, "attachment");
    }

    public String generateDownloadUrl(String storageKey, String disposition) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .responseContentDisposition(disposition)
                .build();

        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(getObjectRequest)
        );

        return presignedRequest.url().toString();
    }
}
