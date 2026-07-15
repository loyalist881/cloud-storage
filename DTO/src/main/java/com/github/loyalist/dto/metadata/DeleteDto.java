package com.github.loyalist.dto.metadata;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDto {
    private String s3Key;
}
