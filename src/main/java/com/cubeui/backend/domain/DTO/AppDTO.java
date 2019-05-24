package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppDTO {

    private Long id;

    private String name;

    private Long customerId;

//    private Long instanceId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
