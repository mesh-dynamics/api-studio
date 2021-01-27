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
  List<DtEnvironment> findDtEnvironmentByAppIdInOrUserId(List<Long> appIds, Long userId);
  @Query(nativeQuery = true , value="select * from devtool_environments where (user_id=:userId Or app_id In :appIds) and global=:global")
  List<DtEnvironment> findDtEnvironmentByUserIdOrAppIdInAndGlobal(@Param("userId") Long userId, @Param("appIds") List<Long> appIds,  @Param("global") boolean global);
}
