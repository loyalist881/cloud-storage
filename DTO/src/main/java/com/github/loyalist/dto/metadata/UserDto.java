package com.github.loyalist.dto.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    @Schema(description = "Идентификатор пользователя",
            example = "42",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Почта пользователя",
            example = "pavlik@mail.ru",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Идентификатор пользователя",
            example = "vilius",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;
}
