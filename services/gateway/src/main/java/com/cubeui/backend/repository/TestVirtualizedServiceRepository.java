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

package com.cubeui.backend.repository;

import com.cubeui.backend.domain.TestVirtualizedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "test_virtualized_services", collectionResourceRel = "test_virtualized_services", itemResourceRel = "test_virtualized_service")
public interface TestVirtualizedServiceRepository extends JpaRepository<TestVirtualizedService, Long> {
    Optional<List<TestVirtualizedService>> findByTestConfigId(Long testConfigId);
    Optional<TestVirtualizedService> findByTestConfigIdAndServiceId(Long testId, Long serviceId);
}
