package com.cubeui.backend.repository;

import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevtoolEnvironmentsRepository extends JpaRepository<DtEnvironment, Long> {
  Optional<List<DtEnvironment>> findDtEnvironmentsByUserId(Long userId);
  Optional<DtEnvironment> findDtEnvironmentById(Long id);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndName(Long userId, String name);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndNameAndIdNot(Long userId, String name, Long id);
  List<DtEnvironment> findDtEnvironmentByNameAndAppIdsAndGlobal(String Name, List<Long> appIds, boolean global);
  List<DtEnvironment> findDtEnvironmentByNameAndAppIdsAndGlobalAndIdNot(String Name, List<Long> appIds, boolean global, Long id);
  List<DtEnvironment> findDtEnvironmentByAppIdsOrUserId(List<Long> appIds, Long userId);
  List<DtEnvironment> findDtEnvironmentByAppIdsOrUserIdAndGlobal(List<Long> appIds, Long userId, boolean global);

}
