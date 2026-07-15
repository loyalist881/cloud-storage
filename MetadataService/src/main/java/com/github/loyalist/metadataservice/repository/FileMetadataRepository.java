package com.github.loyalist.metadataservice.repository;

import com.github.loyalist.dto.metadata.GetAllDto;
import com.github.loyalist.metadataservice.entity.FileMetadataEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {
    @Query(nativeQuery = true,
            value = "SELECT * FROM cloud.metadata u where u.user_id=?1 and u.filename=?2")
    Optional<FileMetadataEntity> findByUserIdAndFilename(Long userId, String filename);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true,
            value = "UPDATE cloud.metadata SET filename = :newFilename WHERE filename = :oldFilename AND user_id = :userId")
    void renameFileMetadata(@Param("oldFilename") String oldFilename, @Param("newFilename") String newFilename, @Param("userId") Long userId);

    @Query(nativeQuery = true,
            value = "SELECT size_file, filename FROM cloud.metadata",
            countQuery = "SELECT count(*) FROM cloud.metadata")
    List<GetAllDto> getAllFilenames(Pageable pageable);
}
