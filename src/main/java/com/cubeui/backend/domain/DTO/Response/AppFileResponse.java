package com.cubeui.backend.domain.DTO.Response;

import com.cubeui.backend.domain.App;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppFileResponse {
  String fileName;
  String filePath;
  App app;
  Object configuration;
  /**TODO
   *Remove in next release, once UI is updated for all customers
   */
  byte[] data;
}
