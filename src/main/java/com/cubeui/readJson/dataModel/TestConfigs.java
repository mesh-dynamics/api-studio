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
    List<String> test_virtualized_services;
    List<String> test_intermediate_services;
}
