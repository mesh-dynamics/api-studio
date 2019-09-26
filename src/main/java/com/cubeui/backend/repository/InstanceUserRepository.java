package com.cubeui.backend.repository;

import com.cubeui.backend.domain.InstanceUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(path = "instance-users", collectionResourceRel = "instance-users", itemResourceRel = "instance-user")
public interface InstanceUserRepository extends JpaRepository<InstanceUser, Long> {

    Optional<List<InstanceUser>> findByUserId(Long userId);

    Optional<List<InstanceUser>> findByInstanceId(Long instanceId);

}
