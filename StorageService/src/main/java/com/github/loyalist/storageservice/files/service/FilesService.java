package com.github.loyalist.storageservice.files.service;

import com.github.loyalist.dto.metadata.DeleteDto;
import com.github.loyalist.dto.metadata.UploadDto;
import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.dto.metadata.RenameDto;
import com.github.loyalist.storageservice.files.exception.FileNotFoundException;
import com.github.loyalist.storageservice.files.exception.FileNotFoundStorageException;
import com.github.loyalist.storageservice.files.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class FilesService {
    private final String bucket;
    private final S3Client s3Client;
    private final WebClient metadataWebClient;

    @Autowired
    public FilesService(S3Client s3Client, @Value("${minio.bucket}") String bucket, WebClient metadataWebClient) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.metadataWebClient = metadataWebClient;
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

    public void uploadFile(MultipartFile file, String s3Key, String filename, Long userId) {
        try {
            if (bucket == null || bucket.isEmpty()) {
                throw new StorageException("Имя S3 bucket не настроено в конфигурации!");
            }

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentDisposition("attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            UploadDto metadataDto = UploadDto.builder()
                    .userId(userId)
                    .filename(filename)
                    .s3Key(s3Key)
                    .sizeFile(file.getSize())
                    .contentType(file.getContentType())
                    .build();

            metadataWebClient.post()
                    .uri("/files/save")
                    .bodyValue(metadataDto)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new StorageException("MetadataService вернул ошибку: " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (IOException e) {
            throw new StorageException("Ошибка чтения потока файла: " + filename, e);
        } catch (S3Exception e) {
            throw new StorageException("Ошибка S3 при загрузке: " + filename, e);
        } catch (Exception e) {
            throw new StorageException("Непредвиденная ошибка при обработке файла: " + e.getMessage(), e);
        }
    }

    public void deleteFile(Long userId, String filename) {
        DeleteDto metadataDeleteDto;
        try {
            metadataDeleteDto = metadataWebClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/delete")
                            .queryParam("userId", userId)
                            .queryParam("filename", filename)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.createException()
                                    .flatMap(error -> Mono.error(new StorageException(
                                            "Ошибка сервиса метаданных (" + error.getStatusCode() + "): " + error.getResponseBodyAsString()
                                    )))
                    )
                    .bodyToMono(DeleteDto.class)
                    .block();
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("MetadataService недоступен или произошла системная ошибка", e);
        }

        if (metadataDeleteDto == null || metadataDeleteDto.getS3Key() == null || metadataDeleteDto.getS3Key().isBlank()) {
            throw new StorageException("Не удалось получить валидный S3-ключ для файла: " + filename);
        }

        try {
            DeleteObjectRequest objectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(metadataDeleteDto.getS3Key())
                    .build();

            s3Client.deleteObject(objectRequest);
        } catch (S3Exception e) {
            throw new StorageException("Ошибка S3 при удалении: " + filename, e);
        }
    }

    public ResponseInputStream<GetObjectResponse> downloadFile(Long userId, String filename) {
        UploadDto metadata;
        try {
            metadata = metadataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/download")
                            .queryParam("userId", userId)
                            .queryParam("filename", filename)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new FileNotFoundException("Файл не найден в БД метаданных: " + errorBody)))
                    )
                    .bodyToMono(UploadDto.class)
                    .block();
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("MetadataService недоступен или произошла системная ошибка", e);
        }

        if (metadata == null || metadata.getS3Key() == null) {
            throw new FileNotFoundException("Метаданные файла не получены для: " + filename);
        }

        try {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(metadata.getS3Key())
                    .build();

            return s3Client.getObject(objectRequest);
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException("Файл не найден: " + filename);
        } catch (S3Exception e) {
            throw new StorageException("Ошибка S3 при скачивании: " + filename, e);
        }
    }

    public List<GetAllDto> getAllFilename(int limit) {
        List<GetAllDto> metadata;
        try {
            metadata = metadataWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/all")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<GetAllDto>>() {})
                    .block();

            return metadata;
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }

    public void renameFile(Long userId, String oldFilename, String newFilename) {
        RenameDto renameDto = RenameDto.builder()
                .userId(userId)
                .oldFilename(oldFilename)
                .newFilename(newFilename)
                .build();

        try {
            metadataWebClient.put()
                    .uri("/files/rename")
                    .bodyValue(renameDto)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, clientResponse ->
                            Mono.error(new FileNotFoundStorageException("Файл '" + oldFilename + "' не найден в БД метаданных"))
                    )
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new StorageException("Ошибка MetadataService: " + errorBody)))
                    )
                    .bodyToMono(String.class)
                    .block();

        } catch (FileNotFoundStorageException | StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }
}
