package com.cubeui.readJson.dataModel;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Customers {
        String name;
        String emailId;
        List<String> domainUrls;
        JiraCredentials jiraCredentials;
        List<Apps> apps;
        List<Users> users;
}
