package com.codeops.fleet.repository;

import com.codeops.fleet.entity.WorkstationProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WorkstationProfile} entities.
 *
 * <p>Provides CRUD operations plus user- and team-scoped queries for listing,
 * counting, finding by name, and locating the default workstation profile.</p>
 */
@Repository
public interface WorkstationProfileRepository extends JpaRepository<WorkstationProfile, UUID> {

    /**
     * Finds all workstation profiles for a user within a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return list of workstation profiles
     */
    List<WorkstationProfile> findByUserIdAndTeamId(UUID userId, UUID teamId);

    /**
     * Finds a workstation profile by user and name.
     *
     * @param userId the user ID
     * @param name   the workstation profile name
     * @return the workstation profile, if found
     */
    Optional<WorkstationProfile> findByUserIdAndName(UUID userId, String name);

    /**
     * Checks whether a workstation profile with the given name exists for a user.
     *
     * @param userId the user ID
     * @param name   the name to check
     * @return true if a profile with that name exists
     */
    boolean existsByUserIdAndName(UUID userId, String name);

    /**
     * Counts the number of workstation profiles for a user within a team.
     *
     * @param userId the user ID
     * @param teamId the team ID
     * @return the count of workstation profiles
     */
    long countByUserIdAndTeamId(UUID userId, UUID teamId);

    /**
     * Finds the default workstation profile for a user within a team.
     *
     * @param userId    the user ID
     * @param teamId    the team ID
     * @param isDefault whether the profile is the default
     * @return the default workstation profile, if one exists
     */
    Optional<WorkstationProfile> findByUserIdAndTeamIdAndIsDefault(UUID userId, UUID teamId, boolean isDefault);
}
