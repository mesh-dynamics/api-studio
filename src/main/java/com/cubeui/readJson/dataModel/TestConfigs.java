package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestConfigs {
    String testConfigName;
    //String description;
    //String gatewayReqSelection;
    //String maxRunTimeMin;
    //String emailId;
    //String slackId;
    String serviceName;
    List<String> paths;
}
