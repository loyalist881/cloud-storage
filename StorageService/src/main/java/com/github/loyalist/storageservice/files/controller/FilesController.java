package com.github.loyalist.storageservice.files.controller;

import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.storageservice.files.service.FilesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NonNull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Операции для работы с файлами")
public class FilesController {
    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @PostMapping(path = "/upload", consumes = MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Сохранение файла",
            description = "Загружаем файл в s3-хранилище")
    @ApiResponse(responseCode = "200", description = "Файл успешно сохранен")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @Parameter(description = "Имя файла")
                                             @RequestParam(value = "filename", required = false) String filename,
                                             @Parameter(description = "ID пользователя-владельца файла", required = true)
                                             @RequestHeader("X-User-Id") Long userId) {
        String originalFilename;
        if (filename != null && !filename.trim().isEmpty()) {
            originalFilename = filename;
        } else {
            originalFilename = file.getOriginalFilename();
        }

        if (originalFilename == null || !originalFilename.contains(".")) {
            originalFilename = "file.bin";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String s3Key = "files/" + UUID.randomUUID() + extension;

        filesService.uploadFile(file, s3Key, originalFilename, userId);
        return ResponseEntity.ok("Файл успешно загружен: " + s3Key);
    }

    @GetMapping("/names")
    @Operation(summary = "Имена файлов",
            description = "Возвращает список имен всех файлов хранящихся в s3-хранилище в количестве 10 имен")
    @ApiResponse(responseCode = "200", description = "Список файлов успешно получен")
    public ResponseEntity<List<GetAllDto>> getAllFilenames(
            @Parameter(description = "Лимит выведенных имен")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<GetAllDto> filenames = filesService.getAllFilename(limit);
        return ResponseEntity.ok(filenames);
    }

    @GetMapping("/download")
    @Operation(summary = "Скачивание файла",
            description = "Скачивание файла на диск с помощью InputStreamResource и HttpHeaders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно скачан"),
            @ApiResponse(responseCode = "404", description = "Файл не найден для указанного пользователя")
    })
    public ResponseEntity<InputStreamResource> downloadFile(@Parameter(description = "ID пользователя-владельца файла", required = true)
                                                            @RequestHeader("X-User-Id") Long userId,
                                                            @Parameter(description = "Имя файла", required = true)
                                                            @RequestParam("filename") String filename) {
        var inputStream = filesService.downloadFile(userId, filename);
        InputStreamResource resource = new InputStreamResource(inputStream);

        var headers = getHttpHeaders(inputStream, filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Удаление файла",
            description = "Удаляет файл из s3-хранилища")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно удален"),
            @ApiResponse(responseCode = "404", description = "Файл для удаления не найден")
    })
    public ResponseEntity<String> deleteFile(@Parameter(description = "ID пользователя-владельца файла", required = true)
                                             @RequestHeader("X-User-Id") Long userId,
                                             @Parameter(description = "Имя файла", required = true)
                                             @RequestParam("filename") String filename) {
        filesService.deleteFile(userId, filename);
        return ResponseEntity.ok("Файл '" + filename + "' успешно удален");
    }

    @PutMapping("/rename")
    @Operation(summary = "Переименовывание файла",
            description = "Получает ответ от 'MetadataService' о том, переименован ли файл или нет")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя файла успешно обновлено"),
            @ApiResponse(responseCode = "404", description = "Файл для переименования не найден")
    })
    public ResponseEntity<String> renameFile(@Parameter(description = "ID пользователя-владельца файла", required = true)
                                             @RequestHeader("X-User-Id") Long userId,
                                             @Parameter(description = "Старое имя файла", required = true)
                                             @RequestParam("oldFilename") String oldFilename,
                                             @Parameter(description = "Новое имя файла", required = true)
                                             @RequestParam("newFilename") String newFilename) {
        filesService.renameFile(userId, oldFilename, newFilename);
        return ResponseEntity.ok("Файл '" + oldFilename + "' успешно переименован в '" + newFilename + "'");
    }

    private static @NonNull HttpHeaders getHttpHeaders(ResponseInputStream<GetObjectResponse> inputStream, String filename) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, inputStream.response().contentType());
        headers.setContentLength(inputStream.response().contentLength());
        return headers;
    }
}
