package com.cubeui.backend.domain.DTO;

import com.cubeui.backend.domain.enums.InstanceName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InstanceDTO {

    private Long id;

    private InstanceName name;

    private String gatewayEndpoint;

    private LocalDateTime createdAt;

}
