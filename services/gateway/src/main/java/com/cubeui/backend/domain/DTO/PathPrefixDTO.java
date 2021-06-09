package com.cubeui.backend.domain.DTO;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathPrefixDTO {
  @NotEmpty
  List<String> prefixes;
  Long serviceId;

}
