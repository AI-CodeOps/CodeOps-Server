package com.codeops.mcp.repository;

import com.codeops.mcp.entity.McpApiToken;
import com.codeops.mcp.entity.enums.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link McpApiToken} entities.
 *
 * <p>Provides CRUD operations plus token lookup by hash,
 * profile-scoped listing with status filtering, and bulk
 * expiration of tokens past their expiry date.</p>
 */
@Repository
public interface McpApiTokenRepository extends JpaRepository<McpApiToken, UUID> {

    /**
     * Finds a token by its SHA-256 hash for authentication.
     *
     * @param tokenHash the SHA-256 hash of the token
     * @return the token if found
     */
    Optional<McpApiToken> findByTokenHash(String tokenHash);

    /**
     * Lists all tokens belonging to a developer profile.
     *
     * @param profileId the developer profile ID
     * @return list of tokens
     */
    List<McpApiToken> findByDeveloperProfileId(UUID profileId);

    /**
     * Lists tokens belonging to a developer profile filtered by status.
     *
     * @param profileId the developer profile ID
     * @param status    the token status to filter by
     * @return list of matching tokens
     */
    List<McpApiToken> findByDeveloperProfileIdAndStatus(UUID profileId, TokenStatus status);

    /**
     * Bulk-expires all active tokens that have passed their expiration date.
     *
     * @param now    the current timestamp
     * @param status the status to set (EXPIRED)
     * @return the number of tokens expired
     */
    @Modifying
    @Query("UPDATE McpApiToken t SET t.status = :status WHERE t.expiresAt < :now AND t.status = 'ACTIVE'")
    int expireTokens(@Param("now") Instant now, @Param("status") TokenStatus status);
}
