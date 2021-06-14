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

package io.cube.agent.logger;

public class CubeDeployment {
    public String app;
    public String instance;
    public String service;
    public String customerId;
    public String version;

    public CubeDeployment(String app , String instance , String service , String customerId,  String version ){
        this.app =app ;
        this.instance = instance;
        this.service = service;
        this.customerId = customerId;
        this.version = version;
    }

}
