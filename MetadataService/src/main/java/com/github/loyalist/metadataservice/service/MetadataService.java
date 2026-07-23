package com.github.loyalist.metadataservice.service;

import com.github.loyalist.dto.metadata.UploadDto;
import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.metadataservice.entity.FileMetadataEntity;
import com.github.loyalist.metadataservice.entity.UserEntity;
import com.github.loyalist.metadataservice.repository.MetadataRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MetadataService {
    private final MetadataRepository metadataRepository;

    @Autowired
    public MetadataService(MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
    }

    @Transactional
    public FileMetadataEntity saveFileMetadata(UploadDto uploadDto) {
        FileMetadataEntity fileMetadataEntity = new FileMetadataEntity();
        fileMetadataEntity.setUserId(uploadDto.getUserId());
        fileMetadataEntity.setS3Key(uploadDto.getS3Key());
        fileMetadataEntity.setSizeFile(uploadDto.getSizeFile());
        fileMetadataEntity.setContentType(uploadDto.getContentType());

        String original = uploadDto.getFilename();
        String finalName = original;
        int counter = 1;

        while (metadataRepository.findByUserIdAndFilename(uploadDto.getUserId(), finalName).isPresent()) {
            finalName = original + " (" + counter + ")";
            counter++;
        }

        fileMetadataEntity.setFilename(finalName);

        return metadataRepository.save(fileMetadataEntity);
    }

    @Transactional(readOnly = true)
    public Optional<FileMetadataEntity> findByUserIdAndFilename(Long userId, String filename) {
        return metadataRepository.findByUserIdAndFilename(userId, filename);
    }

    @Transactional
    public void renameFileMetadata(String oldFilename, String newFilename, Long userId) {
        metadataRepository.renameFileMetadata(oldFilename, newFilename, userId);
    }

    @Transactional(readOnly = true)
    public List<GetAllDto> getAllFilenames(Pageable pageable) {
        return metadataRepository.getAllFilenames(pageable);
    }

    @Transactional
    public void deleteById(Long id) {
        metadataRepository.deleteById(id);
    }

    @Transactional
    public Optional<UserEntity> findByEmail(String email) {
        return metadataRepository.findByEmail(email);
    }
}
