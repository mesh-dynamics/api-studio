package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServiceDTO {

    private Long id;

    private String name;

    private long appId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
