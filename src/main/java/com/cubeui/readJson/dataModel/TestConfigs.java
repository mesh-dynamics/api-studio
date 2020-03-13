package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TestConfigs {
    String testConfigName;
    String serviceName;
    List<String> paths;
}
