package com.cubeui.backend.repository;

import com.cubeui.backend.domain.AppFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "app_files", collectionResourceRel = "app_files", itemResourceRel = "app_files")
public interface AppFileRepository extends JpaRepository<AppFile, Long> {
  Optional<AppFile> findByAppId(Long appIds);
  List<AppFile> findByAppIdIn(List<Long> appIds);
}