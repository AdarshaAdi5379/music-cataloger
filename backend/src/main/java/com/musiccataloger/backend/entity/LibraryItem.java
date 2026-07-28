package com.musiccataloger.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing an album/item saved to a user's library.
 * UUID is used consistently across all entities to prevent ID enumeration vulnerabilities
 * and support distributed architecture.
 *
 * A unique constraint on (user_id, apple_catalog_id) prevents a user from
 * saving the same Apple Music catalogue entry more than once.
 */
@Entity
@Table(
    name = "library_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_library_item_user_apple_catalog",
            columnNames = {"user_id", "apple_catalog_id"}
        )
    },
    indexes = {
        @Index(name = "idx_library_item_user_id", columnList = "user_id"),
        @Index(name = "idx_library_item_apple_catalog_id", columnList = "apple_catalog_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * LAZY loading is used here. The user object is rarely needed when querying a list
     * of library items and should be loaded explicitly when required.
     */
    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank(message = "Apple catalog ID is required")
    @Size(max = 50, message = "Apple catalog ID must not exceed 50 characters")
    @Column(name = "apple_catalog_id", nullable = false, length = 50)
    private String appleCatalogId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @NotBlank(message = "Artist name is required")
    @Size(max = 255, message = "Artist name must not exceed 255 characters")
    @Column(name = "artist_name", nullable = false, length = 255)
    private String artistName;

    @Size(max = 100, message = "Genre must not exceed 100 characters")
    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "track_count")
    private Integer trackCount;

    @Size(max = 2048, message = "Artwork URL must not exceed 2048 characters")
    @Column(name = "artwork_url", length = 2048)
    private String artworkUrl;

    /**
     * User rating stored as a decimal (0.0 – 5.0) for fine-grained control.
     * precision=3, scale=1 stores values such as 4.5 efficiently.
     */
    @DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    @Column(name = "user_rating", precision = 3, scale = 1)
    private BigDecimal userRating;

    @Size(max = 1000, message = "User notes must not exceed 1000 characters")
    @Column(name = "user_notes", length = 1000)
    private String userNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryItem that = (LibraryItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
