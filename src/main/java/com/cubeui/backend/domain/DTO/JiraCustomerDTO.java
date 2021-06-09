package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class JiraCustomerDTO {
    private Long id;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String apiKey;
    @NotEmpty
    private String jiraBaseURL;
    @NotNull
    private Long customerId;
}
