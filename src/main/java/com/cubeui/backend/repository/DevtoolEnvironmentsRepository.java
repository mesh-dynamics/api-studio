package com.cubeui.backend.repository;

import com.cubeui.backend.domain.DtEnvironment;
import com.cubeui.backend.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DevtoolEnvironmentsRepository extends JpaRepository<DtEnvironment, Long> {
  Optional<List<DtEnvironment>> findDtEnvironmentsByUser(User user);
  Optional<DtEnvironment> findDtEnvironmentById(Long id);
  Optional<DtEnvironment> findDtEnvironmentByUserAndName(User user, String name);
  Optional<DtEnvironment> findDtEnvironmentByUserAndNameAndIdNot(User user, String name, Long id);
}
