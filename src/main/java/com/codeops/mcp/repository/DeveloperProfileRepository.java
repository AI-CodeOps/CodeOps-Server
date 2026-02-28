package com.codeops.mcp.repository;

import com.codeops.mcp.entity.DeveloperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DeveloperProfile} entities.
 *
 * <p>Provides CRUD operations plus team-scoped queries for looking up
 * developer profiles by team and user, listing active profiles,
 * and checking existence.</p>
 */
@Repository
public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, UUID> {

    /**
     * Finds a developer profile by team and user.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return the profile if found
     */
    Optional<DeveloperProfile> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Lists all developer profiles for a team.
     *
     * @param teamId the team ID
     * @return list of profiles
     */
    List<DeveloperProfile> findByTeamId(UUID teamId);

    /**
     * Lists all active developer profiles for a team.
     *
     * @param teamId the team ID
     * @return list of active profiles
     */
    List<DeveloperProfile> findByTeamIdAndIsActiveTrue(UUID teamId);

    /**
     * Checks whether a developer profile exists for a given team and user.
     *
     * @param teamId the team ID
     * @param userId the user ID
     * @return true if a profile exists
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
