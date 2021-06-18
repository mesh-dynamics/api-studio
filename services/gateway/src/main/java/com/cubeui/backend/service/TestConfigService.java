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

package com.cubeui.backend.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.DTO.TestConfigDTO;
import com.cubeui.backend.domain.Path;
import com.cubeui.backend.domain.ServiceGroup;
import com.cubeui.backend.domain.TestConfig;
import com.cubeui.backend.domain.TestIntermediateService;
import com.cubeui.backend.domain.TestPath;
import com.cubeui.backend.domain.TestService;
import com.cubeui.backend.domain.TestVirtualizedService;
import com.cubeui.backend.repository.PathRepository;
import com.cubeui.backend.repository.ServiceGroupRepository;
import com.cubeui.backend.repository.ServiceRepository;
import com.cubeui.backend.repository.TestConfigRepository;
import com.cubeui.backend.repository.TestIntermediateServiceRepository;
import com.cubeui.backend.repository.TestPathRepository;
import com.cubeui.backend.repository.TestServiceRepository;
import com.cubeui.backend.repository.TestVirtualizedServiceRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TestConfigService {

	private TestConfigRepository testConfigRepository;
	private ServiceGroupRepository serviceGroupRepository;
	private ServiceRepository serviceRepository;
	private PathRepository pathRepository;
	private TestServiceRepository testServiceRepository;
	private TestPathRepository testPathRepository;
	private TestVirtualizedServiceRepository testVirtualizedServiceRepository;
	private TestIntermediateServiceRepository testIntermediateServiceRepository;

	@Autowired
	public TestConfigService(TestConfigRepository testConfigRepository,
		ServiceGroupRepository serviceGroupRepository, ServiceRepository serviceRepository, PathRepository pathRepository, TestServiceRepository testServiceRepository,
		TestPathRepository testPathRepository,
		TestVirtualizedServiceRepository testVirtualizedServiceRepository,
		TestIntermediateServiceRepository testIntermediateServiceRepository) {
		this.testConfigRepository = testConfigRepository;
		this.serviceGroupRepository = serviceGroupRepository;
		this.serviceRepository = serviceRepository;
		this.pathRepository = pathRepository;
		this.testServiceRepository = testServiceRepository;
		this.testPathRepository = testPathRepository;
		this.testVirtualizedServiceRepository = testVirtualizedServiceRepository;
		this.testIntermediateServiceRepository = testIntermediateServiceRepository;
	}


	@NotNull
	public TestConfig saveTestConfig(TestConfigDTO testConfigDTO, App app) {
		return this.testConfigRepository.save(
			TestConfig.builder()
				.testConfigName(testConfigDTO.getTestConfigName())
				.app(app)
				//.gatewayService(service.get())
				.description(testConfigDTO.getDescription())
				.gatewayReqSelection(testConfigDTO.getGatewayReqSelection())
				.maxRunTimeMin(testConfigDTO.getMaxRunTimeMin())
				.emailId(testConfigDTO.getEmailId())
				.slackId(testConfigDTO.getSlackId())
				.tag(testConfigDTO.getTag())
				.dynamicInjectionConfigVersion(testConfigDTO.getDynamicInjectionConfigVersion())
				.build());
	}

	@NotNull
	public ServiceGroup saveServiceGroup(String serviceGroupName, App app) {
		return this.serviceGroupRepository.save(
			ServiceGroup.builder()
				.app(app)
				.name(serviceGroupName)
				.build());
	}


	@NotNull
	public com.cubeui.backend.domain.Service saveService(String serviceName, App app,
		ServiceGroup serviceGroup) {
		return this.serviceRepository.save(
			com.cubeui.backend.domain.Service.builder()
				.app(app)
				.serviceGroup(serviceGroup)
				.name(serviceName)
				.build());
	}

	@NotNull
	public Path savePath(String path, com.cubeui.backend.domain.Service service) {
		return this.pathRepository.save(
			Path.builder()
				.service(service)
				.path(path)
				.build());
	}

	@NotNull
	public TestService saveTestService(TestConfig testConfig,
		com.cubeui.backend.domain.Service service) {
		return this.testServiceRepository.save(
			TestService.builder()
				.testConfig(testConfig)
				.service(service)
				.build());
	}

	@NotNull
	public TestPath saveTestPath(TestConfig testConfig, Path path) {
		return this.testPathRepository.save(
			TestPath.builder()
				.testConfig(testConfig)
				.path(path)
				.build());
	}

	public TestVirtualizedService saveVirtualizedTestService(TestConfig testConfig,
		com.cubeui.backend.domain.Service service) {
		return this.testVirtualizedServiceRepository.save(
			TestVirtualizedService.builder().service(service).testConfig(testConfig).build());
	}

	@NotNull
	public TestIntermediateService saveTestIntermediateService(TestConfig testConfig,
		com.cubeui.backend.domain.Service service) {
		return this.testIntermediateServiceRepository.save(
			TestIntermediateService.builder().service(service).testConfig(testConfig)
				.build());
	}

}
