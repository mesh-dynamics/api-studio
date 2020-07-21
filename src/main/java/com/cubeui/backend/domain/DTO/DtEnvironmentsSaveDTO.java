package com.cubeui.backend.domain.DTO;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class DtEnvironmentsSaveDTO {
  List<DtEnvironmentDTO> environments;
}
