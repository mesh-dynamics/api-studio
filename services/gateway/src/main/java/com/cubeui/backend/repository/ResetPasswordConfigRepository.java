package com.cubeui.backend.repository;

import com.cubeui.backend.domain.ResetPasswordConfig;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResetPasswordConfigRepository extends JpaRepository<ResetPasswordConfig, Long> {
  Optional<ResetPasswordConfig> findByCustomerId(Long id);
}
