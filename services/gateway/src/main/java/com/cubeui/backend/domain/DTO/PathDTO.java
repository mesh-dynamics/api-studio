package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PathDTO {

    private Long id;

    private String path;

    private Long serviceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
