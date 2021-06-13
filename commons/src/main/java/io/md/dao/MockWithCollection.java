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

package io.md.dao;

import java.util.Optional;

public class MockWithCollection {

  public String replayCollection;
  public String recordCollection;
  public String templateVersion;
  public String runId;
  public Optional<String> dynamicInjectionConfigVersion;
  public boolean isDevtool;
  public Optional<Replay> replay;


  public MockWithCollection(String replayCollection, String recordCollection,
      String templateVersion, String runId, Optional<String> dynamicInjectionConfigVersion, boolean isDevtool , Optional<Replay> replay) {
    this.replayCollection = replayCollection;
    this.recordCollection = recordCollection;
    this.templateVersion = templateVersion;
    this.runId = runId;
    this.dynamicInjectionConfigVersion = dynamicInjectionConfigVersion;
    this.isDevtool = isDevtool;
    this.replay = replay;
  }
}
