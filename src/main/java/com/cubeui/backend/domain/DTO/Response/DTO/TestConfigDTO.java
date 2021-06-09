package com.cubeui.backend.domain.DTO.Response.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private List<String> testPaths;

    private List<String> testServices;

    private List<String> testMockServices;

    private List<String> testIntermediateServices;

    private int maxRunTimeMin;

    private String emailId;

    private String slackId;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private String tag;

    private String dynamicInjectionConfigVersion;

}
