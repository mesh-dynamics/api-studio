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

package io.md.dao.agent.config;

import io.md.dao.Config;

public class ConfigDAO {
  public Integer version;
  public final String customerId;
  public final String app;
  public final String service;
  public final String instanceId;
  public String tag;
  public Config configJson;
  public boolean isLatest;

  public ConfigDAO() {
    this.customerId = null;
    this.version = null;
    this.app = null;
    this.service = null;
    this.instanceId = null;
    this.configJson = null;
    this.isLatest = false;
  }

  public ConfigDAO(String customerId, String app, String service,
      String instanceId, String tag) {
    this.customerId = customerId;
    this.app = app;
    this.service = service;
    this.instanceId = instanceId;
    this.tag = tag;
  }

  public ConfigDAO setConfigJson(Config configJson) {
    this.configJson = configJson;
    return this;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public void setIsLatest(boolean isLatest) {
    this.isLatest = isLatest;
  }
}
