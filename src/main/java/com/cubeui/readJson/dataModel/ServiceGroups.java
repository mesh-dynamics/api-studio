package com.cubeui.readJson.dataModel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ServiceGroups {
    String name;
    List<Services> services;
}
