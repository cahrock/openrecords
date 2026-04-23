package com.openrecords.api.repository;

import com.openrecords.api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access for the users table.
 *
 * Spring Data JPA auto-generates the implementation at startup.
 * We just declare the methods we want and Spring derives the SQL from method names.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Look up a user by email (unique column).
     * Returns Optional because email may not exist.
     */
    Optional<User> findByEmail(String email);

    /**
     * True if a user with this email exists — cheaper than findByEmail when
     * we only need to check existence (e.g., during registration validation).
     */
    boolean existsByEmail(String email);
}