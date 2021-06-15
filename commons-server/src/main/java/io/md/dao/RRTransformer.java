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

import org.json.JSONObject;

public class RRTransformer {
  // TODO: explore and use wiremock's mustache handler capabilities to match and replace values
  // FIX: is there a way to avoid MultivaluedMaps? Just following Request object here.

  // TODO: use ObjectMapper as in Config.java.
  //       however, we then need to structure this class properly

  // types of fields to transform
  // TODO: queryParams, formParams, meta, body
  public JSONObject transforms;
  public RRTransformer(JSONObject hdrs_xfmer) {
    this.transforms = hdrs_xfmer;
  }

  public  RRTransformer() {
    this.transforms = null;
  }
}
