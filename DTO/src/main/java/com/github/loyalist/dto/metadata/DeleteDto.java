package com.github.loyalist.dto.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для удаления файла")
public class DeleteDto {
    @Schema(description = "UUID и расширение",
            example = "7b9e4c1a-8d3f-4b2c-a1e5-9f8c7b6a5d4e.jpg",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String s3Key;
}
