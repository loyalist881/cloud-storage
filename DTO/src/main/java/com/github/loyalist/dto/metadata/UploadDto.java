package com.github.loyalist.dto.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UploadDto {
    @Schema(description = "Идентификатор пользователя-владельца",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Имя файла",
            example = "brdish",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String filename;

    @Schema(description = "UUID и расширение",
            example = "7b9e4c1a-8d3f-4b2c-a1e5-9f8c7b6a5d4e.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String s3Key;

    @Schema(description = "Размер файла",
            example = "112000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sizeFile;

    @Schema(description = "MIME-тип",
            example = "text/plain")
    private String contentType;
}
