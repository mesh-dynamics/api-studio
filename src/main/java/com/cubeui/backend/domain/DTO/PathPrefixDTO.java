package com.cubeui.backend.domain.DTO;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class PathPrefixDTO {
  @NotEmpty
  final List<String> prefixes;
  final Long serviceId;

}
