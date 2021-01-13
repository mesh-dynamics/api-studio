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
  Optional<List<DtEnvironment>> findDtEnvironmentsByUserId(Long userId);
  Optional<DtEnvironment> findDtEnvironmentById(Long id);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndName(Long userId, String name);
  Optional<DtEnvironment> findDtEnvironmentByUserIdAndNameAndIdNot(Long userId, String name, Long id);
  List<DtEnvironment> findDtEnvironmentByNameAndGlobalAndAppIdIn(String Name, boolean global, List<Long> appIds);
  List<DtEnvironment> findDtEnvironmentByNameAndAppIdInAndGlobalAndIdNot(String Name, List<Long> appIds, boolean global, Long id);
  List<DtEnvironment> findDtEnvironmentByAppIdInOrUserId(List<Long> appIds, Long userId);
  @Query(nativeQuery = true , value="select * from devtool_environments where (user_id=:userId Or app_id In :appIds) and global=:global")
  List<DtEnvironment> findDtEnvironmentByUserIdOrAppIdInAndGlobal(@Param("userId") Long userId, @Param("appIds") List<Long> appIds,  @Param("global") boolean global);

}
