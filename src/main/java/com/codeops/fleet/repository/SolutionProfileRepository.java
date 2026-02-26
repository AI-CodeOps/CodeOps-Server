package com.codeops.fleet.repository;

import com.codeops.fleet.entity.SolutionProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SolutionProfile} entities.
 *
 * <p>Provides CRUD operations plus team-scoped queries for listing, counting,
 * finding by name, and locating the default solution profile.</p>
 */
@Repository
public interface SolutionProfileRepository extends JpaRepository<SolutionProfile, UUID> {

    /**
     * Finds all solution profiles belonging to a team.
     *
     * @param teamId the team ID
     * @return list of solution profiles
     */
    List<SolutionProfile> findByTeamId(UUID teamId);

    /**
     * Finds a solution profile by team and name.
     *
     * @param teamId the team ID
     * @param name   the solution profile name
     * @return the solution profile, if found
     */
    Optional<SolutionProfile> findByTeamIdAndName(UUID teamId, String name);

    /**
     * Checks whether a solution profile with the given name exists in a team.
     *
     * @param teamId the team ID
     * @param name   the name to check
     * @return true if a profile with that name exists
     */
    boolean existsByTeamIdAndName(UUID teamId, String name);

    /**
     * Counts the number of solution profiles belonging to a team.
     *
     * @param teamId the team ID
     * @return the count of solution profiles
     */
    long countByTeamId(UUID teamId);

    /**
     * Finds the default solution profile for a team.
     *
     * @param teamId    the team ID
     * @param isDefault whether the profile is the default
     * @return the default solution profile, if one exists
     */
    Optional<SolutionProfile> findByTeamIdAndIsDefault(UUID teamId, boolean isDefault);
}
