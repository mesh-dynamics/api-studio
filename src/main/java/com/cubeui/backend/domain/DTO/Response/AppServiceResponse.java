package com.cubeui.backend.domain.DTO.Response;

import com.cubeui.backend.domain.Service;
import java.util.List;
import lombok.Getter;

@Getter
public class AppServiceResponse {
  final Service service;
  final List<String> prefixes;
  public AppServiceResponse(Service service, List<String> prefixes) {
    this.service = service;
    this.prefixes = prefixes;
  }
}
