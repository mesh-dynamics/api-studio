package com.cubeui.backend.converter;

import com.cubeui.backend.domain.DTO.AppDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.*;

@Component
public class StringAppDTOConverter implements Converter<String, AppDTO> {

  @Autowired
  private ObjectMapper jsonMapper;

  @Override
  @SneakyThrows
  public AppDTO convert(String source) {
    return jsonMapper.readValue(source, AppDTO.class);
  }
}
