package com.cubeui.readJson.dataModel.instances;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstanceData {
  String customerName;
  List<AppData> apps;
}
