package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JiraCustomerDTO {
    private Long id;
    private String userName;
    private String apiKey;
    private String jiraBaseURL;
    private Long customerId;
}
