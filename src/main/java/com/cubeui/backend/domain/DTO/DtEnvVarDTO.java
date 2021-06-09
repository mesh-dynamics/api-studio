package com.cubeui.backend.domain.DTO;

import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DtEnvVarDTO {
  String key;
  String value;
}
