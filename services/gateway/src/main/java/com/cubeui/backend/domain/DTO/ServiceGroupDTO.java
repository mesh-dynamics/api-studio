package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceGroupDTO {

    private Long id;

    private String name;

    private Long appId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
