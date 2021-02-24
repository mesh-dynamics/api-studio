package com.cubeui.backend.repository;

import com.cubeui.backend.domain.AppFilePath;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "app_file_paths", collectionResourceRel = "app_file_paths", itemResourceRel = "app_file_paths")
public interface AppFilePathRepository extends JpaRepository<AppFilePath, Long> {
  Optional<AppFilePath> findByAppId(Long appIds);
  List<AppFilePath> findByAppIdIn(List<Long> appIds);
}
