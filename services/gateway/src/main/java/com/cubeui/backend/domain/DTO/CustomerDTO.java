package com.cubeui.backend.domain.DTO;


import java.util.Set;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CustomerDTO {

    private Long id;

    private String name;

    private String email;

    private Set<String> domainURLs;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
