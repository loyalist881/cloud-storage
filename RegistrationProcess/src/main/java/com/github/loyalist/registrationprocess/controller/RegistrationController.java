package com.github.loyalist.registrationprocess.controller;

import com.github.loyalist.dto.metadata.*;
import com.github.loyalist.registrationprocess.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизация",
            description = "Получает DTO из MetadataService и валидирует пользовательский ввод")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метаданные и id успешно получены"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<String> checkData(@RequestBody UserDto userDto) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword());
        Authentication authentication = registrationService.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assert authentication != null;
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok("Пользователь найден под ID: " + userId);
    }

    @GetMapping("/names")
    @Operation(summary = "Имена файлов",
            description = "Возвращает список имен всех файлов хранящихся в s3-хранилище в количестве 10 имен")
    @ApiResponse(responseCode = "200", description = "Список файлов успешно получен")
    public ResponseEntity<List<GetAllDto>> getAllFilenames(
            @Parameter(description = "Лимит выведенных имен")
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<GetAllDto> filenames = registrationService.getAllFilename(limit);
        return ResponseEntity.ok(filenames);
    }

    @PutMapping("/rename")
    @Operation(summary = "Переименовывание файла",
            description = "Получает ответ от 'MetadataService' о том, переименован ли файл или нет")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Имя файла успешно обновлено"),
            @ApiResponse(responseCode = "404", description = "Файл для переименования не найден")
    })
    public ResponseEntity<String> renameFile(Authentication authentication,
                                             @RequestBody RenameDto renameDto) {
        Long userId = (Long) authentication.getPrincipal();

        String response = registrationService.renameFile(
                userId,
                renameDto.getOldFilename(),
                renameDto.getNewFilename()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/upload", consumes = MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Сохранение файла",
            description = "Отправляет запрос на сервис для сохранения файла в s3-хранилище и в БД")
    @ApiResponse(responseCode = "200", description = "Файл успешно сохранен")
    public ResponseEntity<String> uploadFile(Authentication authentication,
                                             @RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "filename", required = false) String filename) {
        Long userId = (Long) authentication.getPrincipal();
        String id = registrationService.uploadFile(file, userId, filename);
        return ResponseEntity.ok("Файл успешно загружен и имеет ID: " + id);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Удаление файла и его метаданных",
            description = "Отправляет в StorageService и MedataService запрос на удаление")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл и метаданные успешно удалены"),
            @ApiResponse(responseCode = "404", description = "Файл и метаданные для удаления не найден")
    })
    public ResponseEntity<String> deleteFile(Authentication authentication,
                                             @RequestParam(value = "filename", required = false) String filename) {
        Long userId = (Long) authentication.getPrincipal();
        String response = registrationService.deleteFile(userId, filename);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    @Operation(summary = "Скачивание файла",
            description = "Запрашивает ключ в БД и проксирует бинарный поток файла из хранилища клиенту")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл успешно найден и началось скачивание"),
            @ApiResponse(responseCode = "404", description = "Файл не найден для указанного пользователя")
    })
    public ResponseEntity<Resource> downloadFile(Authentication authentication,
                                                    @RequestParam String filename) {
        Long userId = (Long) authentication.getPrincipal();
        Resource resource = registrationService.downloadFile(userId, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
