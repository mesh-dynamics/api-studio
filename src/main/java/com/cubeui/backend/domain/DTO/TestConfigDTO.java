package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestConfigDTO {

    private Long id;

    private String testConfigName;

    private String description;

    private Long appId;

    private String gatewayReqSelection;

    private int maxRunTimeMin;

    private String emailId;

    private String slackId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String tag;

    private String dynamicInjectionConfigVersion;
}
