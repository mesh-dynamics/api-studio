package com.cubeui.backend.repository;

import com.cubeui.backend.domain.Replay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "replays", collectionResourceRel = "replays", itemResourceRel = "replay")
public interface ReplayRepository extends JpaRepository<Replay, Long> {
}
