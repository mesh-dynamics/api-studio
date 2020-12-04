package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppDTO {

    private Long id;

    private String displayName;

    private String customerName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
