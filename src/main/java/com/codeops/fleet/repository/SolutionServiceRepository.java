package com.codeops.fleet.repository;

import com.codeops.fleet.entity.SolutionService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link SolutionService} join entities.
 *
 * <p>Provides queries for finding services within a solution profile,
 * counting services, and checking for duplicate service assignments.</p>
 */
@Repository
public interface SolutionServiceRepository extends JpaRepository<SolutionService, UUID> {

    /**
     * Finds all services belonging to a solution profile, ordered by start order.
     *
     * @param solutionProfileId the solution profile ID
     * @return list of solution services ordered by start order ascending
     */
    List<SolutionService> findBySolutionProfileIdOrderByStartOrderAsc(UUID solutionProfileId);

    /**
     * Checks whether a service profile is already assigned to a solution profile.
     *
     * @param solutionProfileId the solution profile ID
     * @param serviceProfileId  the service profile ID
     * @return true if the service is already in the solution
     */
    boolean existsBySolutionProfileIdAndServiceProfileId(UUID solutionProfileId, UUID serviceProfileId);

    /**
     * Finds a specific service assignment within a solution.
     *
     * @param solutionProfileId the solution profile ID
     * @param serviceProfileId  the service profile ID
     * @return the solution service entry, if found
     */
    Optional<SolutionService> findBySolutionProfileIdAndServiceProfileId(UUID solutionProfileId, UUID serviceProfileId);

    /**
     * Counts the number of services in a solution profile.
     *
     * @param solutionProfileId the solution profile ID
     * @return the count of services
     */
    long countBySolutionProfileId(UUID solutionProfileId);
}
