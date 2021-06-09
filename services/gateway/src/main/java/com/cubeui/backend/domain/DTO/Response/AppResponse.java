package com.cubeui.backend.domain.DTO.Response;

import com.cubeui.backend.domain.App;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppResponse {
  App app;
  Object configuration;
}
