package com.github.loyalist.dto.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для переименования файла")
public class RenameDto {
    @Schema(description = "Идентификатор пользователя-владельца",
            example = "52",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "Текущее имя файла",
            example = "brdish_v1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldFilename;

    @Schema(description = "Новое имя файла",
            example = "brdish_final",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String newFilename;
}
