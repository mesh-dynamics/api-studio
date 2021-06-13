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

package com.cubeui.backend.domain.DTO.Response.Mapper;

import com.cubeui.backend.domain.DTO.Response.DTO.TestConfigDTO;
import com.cubeui.backend.domain.TestConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface TestConfigMapper {

    TestConfigMapper INSTANCE = Mappers.getMapper(TestConfigMapper.class);

    @Mappings({
            @Mapping(source = "app.id", target = "appId"),
            @Mapping(source = "app.name", target = "appName"),
    })
    TestConfigDTO testConfigToTestConfigDTO(TestConfig testConfig);
}
