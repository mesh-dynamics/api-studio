package com.cubeui.backend.domain.DTO.Response.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TestConfigDTO {

    private Long id;

    private String testConfigName;

    private String description;

    private Long appId;

    private String appName;

    private Long gatewayServiceId;

    private String gatewayServiceName;

    private List<String> testPaths;

    private List<String> testMockServices;

    private List<String> testIntermediateServices;

    private int maxRunTimeMin;

    private String emailId;

    private String slackId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
