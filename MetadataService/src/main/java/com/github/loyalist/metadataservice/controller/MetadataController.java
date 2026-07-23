package com.github.loyalist.metadataservice.controller;

import com.github.loyalist.dto.metadata.*;
import com.github.loyalist.metadataservice.entity.FileMetadataEntity;
import com.github.loyalist.metadataservice.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Операции для работы с метаданными файлов")
public class MetadataController {
    private final MetadataService metadataService;

    @Autowired
    public MetadataController(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    @PostMapping("/save")
    @Operation(summary = "Сохранение метаданных файла",
            description = "Возвращает в микросервис 'StorageService' информацию о том, что метаданные файла сохранены в БД")
    @ApiResponse(responseCode = "200", description = "Метаданные успешно сохранены")
    public ResponseEntity<String> saveFileMetadata(@RequestBody UploadDto uploadDto) {
        FileMetadataEntity fileMetadataEntity = metadataService.saveFileMetadata(uploadDto);
        return ResponseEntity.ok("Успешно сохранено с ID: " + fileMetadataEntity.getId());
    }

    @GetMapping("/download")
    @Operation(summary = "Скачивание метаданных файла",
            description = "Возвращает метаданные файла для скачивания")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метаданные файла успешно получены"),
            @ApiResponse(responseCode = "404", description = "Файл не найден для указанного пользователя")
    })
    public ResponseEntity<UploadDto> downloadFileMetadata(
            @Parameter(description = "ID пользователя-владельца файла", required = true) @RequestParam("userId") Long userId,
            @Parameter(description = "Имя файла", required = true) @RequestParam("filename") String filename) {
        return metadataService.findByUserIdAndFilename(userId, filename)
                .map(entity -> {
                    UploadDto uploadDto = UploadDto.builder()
                            .userId(entity.getUserId())
                            .filename(entity.getFilename())
                            .s3Key(entity.getS3Key())
                            .sizeFile(entity.getSizeFile())
                            .contentType(entity.getContentType())
                            .build();
                    return ResponseEntity.ok(uploadDto);
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/rename")
    @Operation(summary = "Переименовывание метаданных файла",
            description = "Возвращает информацию об успешном переименовывании имени файла в БД")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя файла успешно обновлено"),
            @ApiResponse(responseCode = "404", description = "Файл для переименования не найден")
    })
    public ResponseEntity<String> renameFileMetadata(@RequestBody RenameDto renameDto) {
        metadataService.renameFileMetadata(renameDto.getOldFilename(), renameDto.getNewFilename(), renameDto.getUserId());
        return metadataService.findByUserIdAndFilename(renameDto.getUserId(), renameDto.getNewFilename())
                .map(file -> ResponseEntity.ok("Файл успешно переименован на: " + file.getFilename()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    @Operation(summary = "Имена файлов в метаданных",
            description = "Возвращает список имен всех файлов хранящихся в БД")
    @ApiResponse(responseCode = "200", description = "Список файлов успешно получен")
    public ResponseEntity<List<GetAllDto>> getAllFileMetadata(@ParameterObject @PageableDefault(page = 0, size = 10) Pageable pageable) {
        List<GetAllDto> filenames = metadataService.getAllFilenames(pageable);
        return ResponseEntity.ok(filenames);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Удаление метаданных файла",
            description = "Удаляет запись из БД и возвращает метаданные файла для удаления")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метаданные успешно удалены"),
            @ApiResponse(responseCode = "404", description = "Файл для удаления не найден")
    })
    public ResponseEntity<DeleteDto> deleteFileMetadata(
            @Parameter(description = "ID пользователя-владельца файла", required = true) @RequestParam("userId") Long userId,
            @Parameter(description = "Имя файла", required = true) @RequestParam("filename") String filename) {
        return metadataService.findByUserIdAndFilename(userId, filename)
                .map(entity -> {
                    DeleteDto metadataDeleteDto = DeleteDto.builder()
                            .s3Key(entity.getS3Key())
                            .build();
                    metadataService.deleteById(entity.getId());
                    return ResponseEntity.ok(metadataDeleteDto);
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/login")
    @Operation(summary = "Получение пользователя по email",
            description = "Получает email от 'RegistrationService' и возвращает id, email и зашифрованный пароль")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<UserDto> findEmailAndPassword(@RequestBody UserDto userDto) {
        return metadataService.findByEmail(userDto.getEmail())
        .map(user -> {
            UserDto responseDto = UserDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .build();

            return ResponseEntity.ok(responseDto);
        })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
