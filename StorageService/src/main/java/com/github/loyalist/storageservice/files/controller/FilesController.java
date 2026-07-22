package com.github.loyalist.storageservice.files.controller;

import com.github.loyalist.storageservice.files.service.FilesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Операции для работы с файлами")
public class FilesController {
    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @PostMapping(path = "/upload")
    @Operation(summary = "Сохранение файла",
            description = "Загружаем файл в s3-хранилище")
    @ApiResponse(responseCode = "200", description = "Файл успешно сохранен")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String s3Key = filesService.uploadFile(file);
        return ResponseEntity.ok(s3Key);
    }

    @GetMapping("/download")
    @Operation(summary = "Скачивание файла",
            description = "Скачивание файла на диск с помощью InputStreamResource и HttpHeaders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно скачан"),
            @ApiResponse(responseCode = "404", description = "Файл не найден для указанного пользователя")
    })
    public ResponseEntity<Resource> downloadFile(@RequestParam("s3Key") String s3Key) {
        InputStream inputStream = filesService.downloadFile(s3Key);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Удаление файла",
            description = "Удаляет файл из s3-хранилища")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно удален"),
            @ApiResponse(responseCode = "404", description = "Файл для удаления не найден")
    })
    public ResponseEntity<String> deleteFile(@RequestParam("s3Key") String s3Key) {
        filesService.deleteFile(s3Key);
        return ResponseEntity.ok("Файл успешно удален");
    }

    private static @NonNull HttpHeaders getHttpHeaders(ResponseInputStream<GetObjectResponse> inputStream, String s3Key) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; s3Key=\"" + s3Key + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, inputStream.response().contentType());
        headers.setContentLength(inputStream.response().contentLength());
        return headers;
    }
}
