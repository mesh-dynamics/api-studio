package com.cubeui.backend.repository;

import com.cubeui.backend.domain.UserOldPasswords;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOldPasswordsRepository extends JpaRepository<UserOldPasswords, Long> {
  List<UserOldPasswords> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
