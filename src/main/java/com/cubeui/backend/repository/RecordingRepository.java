package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "recordings", collectionResourceRel = "recordings", itemResourceRel = "recording")
public interface RecordingRepository extends JpaRepository<Recording, Long> {
}
