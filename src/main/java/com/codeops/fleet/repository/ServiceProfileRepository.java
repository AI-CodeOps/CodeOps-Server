package com.codeops.fleet.repository;

import com.codeops.fleet.entity.ServiceProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ServiceProfile} entities.
 *
 * <p>Provides CRUD operations plus team-scoped queries for listing profiles,
 * filtering by enabled status, and linking to Registry service registrations.</p>
 */
@Repository
public interface ServiceProfileRepository extends JpaRepository<ServiceProfile, UUID> {

    /**
     * Finds all service profiles belonging to a team.
     *
     * @param teamId the team ID
     * @return list of service profiles
     */
    List<ServiceProfile> findByTeamId(UUID teamId);

    /**
     * Finds a service profile by team and service name.
     *
     * @param teamId      the team ID
     * @param serviceName the service name
     * @return the service profile, if found
     */
    Optional<ServiceProfile> findByTeamIdAndServiceName(UUID teamId, String serviceName);

    /**
     * Finds service profiles for a team filtered by enabled status.
     *
     * @param teamId    the team ID
     * @param isEnabled whether the profile is enabled
     * @return list of matching service profiles
     */
    List<ServiceProfile> findByTeamIdAndIsEnabled(UUID teamId, boolean isEnabled);

    /**
     * Finds a service profile linked to a specific Registry service registration.
     *
     * @param serviceRegistrationId the service registration ID
     * @return the linked service profile, if found
     */
    Optional<ServiceProfile> findByServiceRegistrationId(UUID serviceRegistrationId);

    /**
     * Checks whether a service profile with the given name exists in a team.
     *
     * @param teamId      the team ID
     * @param serviceName the service name to check
     * @return true if a profile with that name exists
     */
    boolean existsByTeamIdAndServiceName(UUID teamId, String serviceName);
}
