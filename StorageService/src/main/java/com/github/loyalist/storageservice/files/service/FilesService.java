package com.github.loyalist.storageservice.files.service;

import com.github.loyalist.storageservice.files.exception.FileNotFoundException;
import com.github.loyalist.storageservice.files.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.UUID;

@Slf4j
@Service
public class FilesService {
    private final String bucket;
    private final S3Client s3Client;

    @Autowired
    public FilesService(S3Client s3Client, @Value("${minio.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initBucket() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();

            s3Client.headBucket(headBucketRequest);
            log.info("Корзина '{}' успешно найдена", bucket);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn("Корзина '{}' не существует", bucket);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Корзина '{}' успешно создана", bucket);
            } else {
                log.error("Ошибка S3 при проверке/создании хранилища", e);
                throw new StorageException("Ошибка S3 при создании хранилища", e);
            }
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                originalFilename = "file.bin";
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String s3Key = "files/" + UUID.randomUUID() + extension;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentDisposition("attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return s3Key;
        } catch (Exception e) {
            throw new StorageException("Ошибка при загрузке в S3: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest objectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(objectRequest);
        } catch (S3Exception e) {
            throw new StorageException("Ошибка S3 при удалении файла", e);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(String s3Key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build();

            return s3Client.getObject(objectRequest);
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException("Файл не найден");
        } catch (S3Exception e) {
            throw new StorageException("Ошибка S3 при скачивании", e);
        }
    }
}
