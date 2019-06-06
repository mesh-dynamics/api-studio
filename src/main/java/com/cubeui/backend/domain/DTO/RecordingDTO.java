package com.cubeui.backend.domain.DTO;


import com.cubeui.backend.domain.enums.RecordingStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecordingDTO {

    private Long id;

    private Long appId;

    private Long instanceId;

    private String collectionName;

    private RecordingStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}
