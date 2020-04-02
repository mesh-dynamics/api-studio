package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Apps {
    String name;
    List<Instances> instances;
    List<ServiceGroups> serviceGroups;
    List<TestConfigs> testConfigs;
    List<ServiceGraphs> serviceGraphs;
}
