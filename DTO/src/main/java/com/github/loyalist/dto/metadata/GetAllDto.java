package com.github.loyalist.dto.metadata;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class GetAllDto {
    private Long sizeFile;
    private String fileName;
}
