package com.cubeui.backend.domain.DTO;

import com.cubeui.backend.domain.enums.TemplateType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CompareTemplateDTO {

    private Long id;

    private Long testId;

    private String path;

    private String template;

    private TemplateType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
