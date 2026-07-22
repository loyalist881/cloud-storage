package com.github.loyalist.registrationprocess.service;

import com.github.loyalist.dto.metadata.*;
import com.github.loyalist.registrationprocess.exception.FileNotFoundMetadataException;
import com.github.loyalist.registrationprocess.exception.RegistrationException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@Service
public class RegistrationService implements AuthenticationProvider {
    private final RestClient metadataRestClient;
    private final RestClient storageRestClient;

    @Autowired
    public RegistrationService(RestClient metadataRestClient, RestClient storageRestClient) {
        this.metadataRestClient = metadataRestClient;
        this.storageRestClient = storageRestClient;
    }

    public UserDto checkData(String email, String password) {
        UserDto userDto = UserDto.builder()
                .email(email)
                .password(password)
                .build();
        try {
            return metadataRestClient.post()
                    .uri("/files/login")
                    .body(userDto)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new RegistrationException("Пользователь с такими данными не найден!");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(UserDto.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }

    public String uploadFile(MultipartFile file, Long userId, String filename) {
        MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        String s3Key;
        try {
            s3Key = storageRestClient.post()
                    .uri("/files/upload")
                    .contentType(MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка StorageService: " + errorBody);
                    })
                    .body(String.class);
        } catch (Exception e) {
            throw new RegistrationException("Ошибка при сохранении файла в хранилище", e);
        }

        String finalFilename;
        if (filename != null && !filename.isBlank()) {
            finalFilename = filename;
        } else {
            finalFilename = file.getOriginalFilename();
        }

        UploadDto metadataDto = UploadDto.builder()
                .userId(userId)
                .filename(finalFilename)
                .s3Key(s3Key)
                .sizeFile(file.getSize())
                .contentType(file.getContentType())
                .build();

        try {
            return metadataRestClient.post()
                    .uri("/files/save")
                    .body(metadataDto)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(String.class);
        } catch (Exception e) {
            throw new RegistrationException("Ошибка при сохранении метаданных файла", e);
        }
    }

    public List<GetAllDto> getAllFilename(int limit) {
        List<GetAllDto> metadata;
        try {
            metadata = metadataRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/all")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return metadata;
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }

    public String renameFile(Long userId, String oldFilename, String newFilename) {
        RenameDto renameDto = RenameDto.builder()
                .userId(userId)
                .oldFilename(oldFilename)
                .newFilename(newFilename)
                .build();

        try {
            return metadataRestClient.put()
                    .uri("/files/rename")
                    .body(renameDto)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new FileNotFoundMetadataException("Файл '" + oldFilename + "' не найден в БД метаданных");
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(String.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }

    public String deleteFile(Long userId, String filename) {
        DeleteDto deleteDto;
        try {
            deleteDto = metadataRestClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/delete")
                            .queryParam("userId", userId)
                            .queryParam("filename", filename)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(DeleteDto.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }

        if (deleteDto == null || deleteDto.getS3Key() == null || deleteDto.getS3Key().isBlank()) {
            throw new RegistrationException("Не удалось получить валидный S3-ключ для файла: " + filename);
        }

        try {
            return storageRestClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/delete")
                            .queryParam("s3Key", deleteDto.getS3Key())
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(String.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("StorageService недоступен или произошла системная ошибка", e);
        }
    }

    public Resource downloadFile(Long userId, String filename) {
        UploadDto metadata;
        try {
            metadata = metadataRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/download")
                            .queryParam("userId", userId)
                            .queryParam("filename", filename)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(UploadDto.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }

        if (metadata == null || metadata.getS3Key() == null) {
            throw new FileNotFoundMetadataException("Метаданные файла не получены для: " + filename);
        }

        try {
            return storageRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/files/download")
                            .queryParam("s3Key", metadata.getS3Key())
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String errorBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RegistrationException("Ошибка MetadataService: " + errorBody);
                    })
                    .body(Resource.class);
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }


    // Настройка Authentication
    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String emailA = authentication.getName();
        String passwordA = authentication.getCredentials().toString();

        UserDto userDto = checkData(emailA, passwordA);

        return new UsernamePasswordAuthenticationToken(
                userDto.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
