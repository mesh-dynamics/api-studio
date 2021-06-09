package com.cubeui.backend.domain.DTO;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class DtEnvironmentDTO {
  Long id;

  @NotEmpty
  String name;

  List<DtEnvVarDTO> vars = new ArrayList<>();
  List<DtEnvServiceHostDTO> dtEnvServiceHosts = new ArrayList<>();
  List<DtEnvServiceCollectionDTO> dtEnvServiceCollections = new ArrayList<>();
  Long appId;
  boolean global=false;
}
