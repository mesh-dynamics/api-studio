package com.cubeui.backend.service.utils;

import com.cubeui.backend.web.exception.InvalidDataException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {

  public static String getDomainFromEmail(String email) {
    try {
      String[] emailSplit = email.split("@");
      String domain = emailSplit[1];
      return domain;
    } catch (Exception e) {
      log.error("The email doesn't have '@' field in it");
      throw new InvalidDataException("The email doesn't have '@' field in it");
    }
  }
}
