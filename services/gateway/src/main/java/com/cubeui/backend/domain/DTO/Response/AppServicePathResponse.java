package com.cubeui.backend.domain.DTO.Response;

import java.util.List;

import com.cubeui.backend.domain.Service;

import lombok.Getter;

@Getter
public class AppServicePathResponse {
  final Service service;
  final List<String> paths;
  public AppServicePathResponse(Service service, List<String> paths) {
    this.service = service;
    this.paths = paths;
  }
}
