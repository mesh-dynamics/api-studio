package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestDTO {

    private Long id;

    private String testConfigName;

    private String description;

    private Long collectionId;

    private Long gatewayServiceId;

    private String gatewayPathSelection;

    private String endpoint;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
