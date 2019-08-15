package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InstanceDTO {

    private Long id;

    private String name;

    private Long appId;

    private String gatewayEndpoint;

    private LocalDateTime createdAt;

}
