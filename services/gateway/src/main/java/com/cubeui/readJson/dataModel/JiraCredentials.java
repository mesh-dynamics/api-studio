package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JiraCredentials {
    private String userName;
    private String apiKey;
    private String jiraBaseURL;
}