package com.cubeui.backend.domain.DTO;

import com.cubeui.backend.domain.enums.ReplayStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReplayDTO {

    private Long id;

    private String replayName;

    private Long testId;

    private Long collectionId;

    private ReplayStatus status;

    private int reqCount;

    private int reqSent;

    private int reqFailed;

    private String analysis;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private Double sampleRate;
}
