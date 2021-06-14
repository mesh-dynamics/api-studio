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

package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TestConfigDTO {

    private Long id;

    private String testConfigName;

    private String description;

    private Long appId;

    private String gatewayReqSelection;

    private int maxRunTimeMin;

    private String emailId;

    private String slackId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String tag;

    private String dynamicInjectionConfigVersion;
}
