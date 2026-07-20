package com.github.loyalist.dto.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Данные для получения всех имен файла")
public class GetAllDto {
    @Schema(description = "Размер файла",
            example = "112000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sizeFile;

    @Schema(description = "Идентификатор пользователя-владельца",
            example = "cheremsha",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
}
