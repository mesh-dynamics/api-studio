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

import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface DevtoolEnvironmentsRepository extends JpaRepository<DtEnvironment, Long> {
  Optional<DtEnvironment> findDtEnvironmentById(Long id);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndName(Long userId, String name);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndNameAndAppId(Long userId, String name, Long appId);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndNameAndAppIdAndIdNot(Long userId, String name,Long appId, Long id);
  List<DtEnvironment> findDtEnvironmentByNameAndGlobalAndAppId(String Name, boolean global, Long appId);
  List<DtEnvironment> findDtEnvironmentByNameAndAppIdAndGlobalAndIdNot(String Name, Long appId, boolean global, Long id);
  List<DtEnvironment> findDtEnvironmentByAppIdInAndUserId(List<Long> appIds, Long userId);
  List<DtEnvironment> findDtEnvironmentByUserIdAndAppIdInAndGlobal(@Param("userId") Long userId, @Param("appIds") List<Long> appIds,  @Param("global") boolean global);
}
