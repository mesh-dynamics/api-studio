package com.cubeui.backend.repository;

import com.cubeui.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsernameIgnoreCaseAndCustomerId(String userName, long customerId);

    Optional<User> findByResetKey(String resetKey);

    List<User> findAllByActivatedIsFalseAndCreatedAtBefore(LocalDateTime dateTime);
    Optional<List<User>> findByCustomerId(Long customerId);
    void deleteByCustomerId(long customerId);
}
