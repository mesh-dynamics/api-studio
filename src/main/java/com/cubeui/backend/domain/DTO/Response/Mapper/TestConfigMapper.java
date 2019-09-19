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
            @Mapping(source = "gatewayService.id", target = "gatewayServiceId"),
            @Mapping(source = "gatewayService.name", target = "gatewayServiceName")
    })
    TestConfigDTO testConfigToTestConfigDTO(TestConfig testConfig);
}
