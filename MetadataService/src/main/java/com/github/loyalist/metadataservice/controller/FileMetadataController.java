package com.github.loyalist.metadataservice.controller;

import com.github.loyalist.dto.metadata.DeleteDto;
import com.github.loyalist.dto.metadata.UploadDto;
import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.dto.metadata.RenameDto;
import com.github.loyalist.metadataservice.entity.FileMetadataEntity;
import com.github.loyalist.metadataservice.service.FileMetadataService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/files")
@Tag(name = "Files", description = "Операции для работы с метаданными файлов")
@ApiResponses(@ApiResponse(responseCode = "200", useReturnTypeSchema = true))
public class FileMetadataController {
    private final FileMetadataService fileMetadataService;

    @Autowired
    public FileMetadataController(FileMetadataService fileMetadataService) {
        this.fileMetadataService = fileMetadataService;
    }

    @PostMapping("/save")
    public ResponseEntity<String> saveFileMetadata(@RequestBody UploadDto uploadDto) {
        FileMetadataEntity fileMetadataEntity = fileMetadataService.saveFileMetadata(uploadDto);
        return ResponseEntity.ok("Успешно сохранено с ID: " + fileMetadataEntity.getId());
    }

    @GetMapping("/download")
    public ResponseEntity<UploadDto> downloadFileMetadata(@RequestParam("userId") Long userId,
                                                          @RequestParam("filename") String filename) {
        return fileMetadataService.findByUserIdAndFilename(userId, filename)
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
    public ResponseEntity<String> renameFileMetadata(@RequestBody RenameDto renameDto) {
        fileMetadataService.renameFileMetadata(renameDto.getOldFilename(), renameDto.getNewFilename(), renameDto.getUserId());
        return fileMetadataService.findByUserIdAndFilename(renameDto.getUserId(), renameDto.getNewFilename())
                .map(file -> ResponseEntity.ok("Файл успешно переименован на: " + file.getFilename()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<GetAllDto>> getAllFileMetadata(@PageableDefault(page = 0, size = 10) Pageable pageable) {
        List<GetAllDto> filenames = fileMetadataService.getAllFilenames(pageable);
        return ResponseEntity.ok(filenames);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<DeleteDto> deleteFileMetadata(@RequestParam("userId") Long userId,
                                                        @RequestParam("filename") String filename) {
        return fileMetadataService.findByUserIdAndFilename(userId, filename)
                .map(entity -> {
                    DeleteDto metadataDeleteDto = DeleteDto.builder()
                            .s3Key(entity.getS3Key())
                            .build();
                    fileMetadataService.deleteById(entity.getId());
                    return ResponseEntity.ok(metadataDeleteDto);
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
