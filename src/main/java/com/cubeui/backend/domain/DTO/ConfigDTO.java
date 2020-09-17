package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigDTO {
  String customer;
  String app;
  String service;
  String configType;
  String key;
  String value;
  boolean authenticate;
}
