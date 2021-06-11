package com.cubeio.logcollector.repository;

import com.cubeio.logcollector.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndCustomerId(String userName, long customerId);

    Optional<User> findByResetKey(String resetKey);

    List<User> findAllByActivatedIsFalseAndCreatedAtBefore(LocalDateTime dateTime);
    Optional<List<User>> findByCustomerId(Long customerId);
    void deleteByCustomerId(long customerId);
}
