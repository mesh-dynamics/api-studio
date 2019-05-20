package com.cubeui.backend.repository;

import com.cubeui.backend.domain.CompareTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "compare_templates", collectionResourceRel = "compare_templates", itemResourceRel = "compare_template")
public interface CompareTemplateRepository extends JpaRepository<CompareTemplate, Long> {
}
