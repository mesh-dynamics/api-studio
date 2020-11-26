package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppDTO {

    private Long id;

    private String name;

    private String displayName;

    private Long customerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
