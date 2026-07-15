package com.github.loyalist.dto.metadata;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UploadDto {
    private Long userId;
    private String filename;
    private String s3Key;
    private Long sizeFile;
    private String contentType;
}
