package com.github.loyalist.metadataservice.service;

import com.github.loyalist.dto.metadata.UploadDto;
import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.metadataservice.entity.FileMetadataEntity;
import com.github.loyalist.metadataservice.repository.FileMetadataRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FileMetadataService {
    private final FileMetadataRepository fileMetadataRepository;

    @Autowired
    public FileMetadataService(FileMetadataRepository fileMetadataRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Transactional
    public FileMetadataEntity saveFileMetadata(UploadDto uploadDto) {
        FileMetadataEntity fileMetadataEntity = new FileMetadataEntity();
        fileMetadataEntity.setUserId(uploadDto.getUserId());
        fileMetadataEntity.setFilename(uploadDto.getFilename());
        fileMetadataEntity.setS3Key(uploadDto.getS3Key());
        fileMetadataEntity.setSizeFile(uploadDto.getSizeFile());
        fileMetadataEntity.setContentType(uploadDto.getContentType());

        return fileMetadataRepository.save(fileMetadataEntity);
    }

    @Transactional(readOnly = true)
    public Optional<FileMetadataEntity> findByUserIdAndFilename(Long userId, String filename) {
        return fileMetadataRepository.findByUserIdAndFilename(userId, filename);
    }

    @Transactional
    public void renameFileMetadata(String oldFilename, String newFilename, Long userId) {
        fileMetadataRepository.renameFileMetadata(oldFilename, newFilename, userId);
    }

    @Transactional(readOnly = true)
    public List<GetAllDto> getAllFilenames(Pageable pageable) {
        return fileMetadataRepository.getAllFilenames(pageable);
    }

    @Transactional
    public void deleteById(Long id) {
        fileMetadataRepository.deleteById(id);
    }
}
