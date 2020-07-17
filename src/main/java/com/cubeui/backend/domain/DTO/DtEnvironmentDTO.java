package com.cubeui.backend.domain.DTO;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DtEnvironmentDTO {
  Long id;

  @NotEmpty
  String name;

  List<DtEnvVarDTO> vars = new ArrayList<>();
}
