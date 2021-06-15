/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
