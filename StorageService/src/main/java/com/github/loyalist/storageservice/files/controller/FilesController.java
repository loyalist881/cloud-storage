package com.github.loyalist.storageservice.files.controller;

import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.storageservice.files.service.FilesService;
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
@ApiResponses(@ApiResponse(responseCode = "200", useReturnTypeSchema = true))
public class FilesController {
    private final FilesService filesService;

    public FilesController(FilesService filesService) {
        this.filesService = filesService;
    }

    @PostMapping(path = "/upload", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "filename", required = false) String filename,
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
    public ResponseEntity<List<GetAllDto>> getAllFilenames(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<GetAllDto> filenames = filesService.getAllFilename(limit);
        return ResponseEntity.ok(filenames);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(@RequestHeader("X-User-Id") Long userId,
                                                            @RequestParam("filename") String filename) {
        var inputStream = filesService.downloadFile(userId, filename);
        InputStreamResource resource = new InputStreamResource(inputStream);

        var headers = getHttpHeaders(inputStream, filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam("filename") String filename) {
        filesService.deleteFile(userId, filename);
        return ResponseEntity.ok("Файл '" + filename + "' успешно удален");
    }

    @PutMapping("/rename")
    public ResponseEntity<String> renameFile(@RequestHeader("X-User-Id") Long userId,
                                             @RequestParam("oldFilename") String oldFilename,
                                             @RequestParam("newFilename") String newFilename) {
        filesService.renameFile(userId, oldFilename, newFilename);
        return ResponseEntity.ok("Файл '" + oldFilename + "' успешно переименован в '" + newFilename + "'");
    }

    // С помощью кастомного getHttpHeaders мы вязли contentLength и contentType напрямую из метаданных Minio
    // Это значит, что файл не читается дважды, память не расходуется
    private static @NonNull HttpHeaders getHttpHeaders(ResponseInputStream<GetObjectResponse> inputStream, String filename) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, inputStream.response().contentType());
        headers.setContentLength(inputStream.response().contentLength());
        return headers;
    }
}
