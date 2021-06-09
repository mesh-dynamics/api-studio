package com.cubeui.backend.repository;

import com.cubeui.backend.domain.PathPrefix;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PathPrefixRepository extends JpaRepository<PathPrefix, Long> {
  Optional<PathPrefix> findByPrefixAndServiceId(String prefix, Long serviceId);
  Optional<List<PathPrefix>> findByServiceId(Long serviceId);
}
