package com.musiccataloger.backend.repository;

import com.musiccataloger.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence layer for {@link User} entities.
 * Backed by Spring Data JPA — no boilerplate implementation required.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique email address.
     *
     * @param email the email to search for (case-sensitive)
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email address already exists.
     * Preferred over {@link #findByEmail} for existence checks — avoids loading the full entity.
     *
     * @param email the email to check
     * @return {@code true} if a user with this email exists
     */
    boolean existsByEmail(String email);
}
