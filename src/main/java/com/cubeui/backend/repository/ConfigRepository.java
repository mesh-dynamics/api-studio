package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Config;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "configs", collectionResourceRel = "configs", itemResourceRel = "config")
public interface ConfigRepository extends JpaRepository<Config, Long> {
  Optional<Config> findByIdAndUserId(Long id, String userId);
  Optional<Config> findByKeyAndCustomerAndAppAndConfigTypeAndUserId(String key,String customer, String app, String configType, String userId);
}
