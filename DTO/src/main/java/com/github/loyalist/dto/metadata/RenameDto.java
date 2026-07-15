package com.github.loyalist.dto.metadata;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RenameDto {
    private Long userId;
    private String oldFilename;
    private String newFilename;
}
