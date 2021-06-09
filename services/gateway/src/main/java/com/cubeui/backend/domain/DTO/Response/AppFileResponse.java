package com.cubeui.backend.domain.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppFileResponse {
  String fileName;
  String fileType;
  byte[] data;
  String appName;

}
