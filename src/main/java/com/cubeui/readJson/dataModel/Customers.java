package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Customers {
        String name;
        String emailId;
        String domainUrl;
        JiraCredentials jiraCredentials;
        List<Apps> apps;
        List<Users> users;
}
