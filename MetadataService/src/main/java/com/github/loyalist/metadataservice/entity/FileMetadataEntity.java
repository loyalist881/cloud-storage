package com.github.loyalist.metadataservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "cloud", name = "metadata")
public class FileMetadataEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "filename", nullable = false, length = 100)
    private String filename;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "size_file", nullable = false)
    private Long sizeFile;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
}
