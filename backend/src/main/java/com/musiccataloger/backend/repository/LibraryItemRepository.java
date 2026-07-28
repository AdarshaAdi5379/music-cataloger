package com.musiccataloger.backend.repository;

import com.musiccataloger.backend.entity.LibraryItem;
import com.musiccataloger.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence layer for {@link LibraryItem} entities.
 * Backed by Spring Data JPA — no boilerplate implementation required.
 *
 * All queries are scoped to a {@link User} to enforce ownership boundaries
 * at the repository level, preventing cross-user data access.
 */
@Repository
public interface LibraryItemRepository extends JpaRepository<LibraryItem, UUID> {

    /**
     * Retrieves all library items belonging to the given user.
     *
     * @param user the owning user
     * @return list of library items; empty list if the user has none
     */
    List<LibraryItem> findAllByUser(User user);

    /**
     * Checks whether the user already has an item with the given Apple catalogue ID
     * saved in their library. Used to enforce the unique constraint at the application
     * layer before attempting a database insert.
     *
     * @param user           the owning user
     * @param appleCatalogId the Apple Music catalogue ID to check
     * @return {@code true} if the item is already in the user's library
     */
    boolean existsByUserAndAppleCatalogId(User user, String appleCatalogId);

    /**
     * Retrieves a specific library item by its primary key, scoped to the given user.
     * Prevents users from accessing items that belong to other users.
     *
     * @param user the owning user
     * @param id   the library item UUID
     * @return an {@link Optional} containing the item if found and owned by the user
     */
    Optional<LibraryItem> findByUserAndId(User user, UUID id);

    /**
     * Deletes a specific library item by its primary key, scoped to the given user.
     * Prevents users from deleting items that belong to other users.
     *
     * @param user the owning user
     * @param id   the library item UUID
     */
    void deleteByUserAndId(User user, UUID id);
}
