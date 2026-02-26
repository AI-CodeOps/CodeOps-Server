package com.codeops.fleet.repository;

import com.codeops.fleet.entity.WorkstationSolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WorkstationSolution} join entities.
 *
 * <p>Provides queries for finding solutions within a workstation profile,
 * counting solutions, and checking for duplicate solution assignments.</p>
 */
@Repository
public interface WorkstationSolutionRepository extends JpaRepository<WorkstationSolution, UUID> {

    /**
     * Finds all solutions belonging to a workstation profile, ordered by start order.
     *
     * @param workstationProfileId the workstation profile ID
     * @return list of workstation solutions ordered by start order ascending
     */
    List<WorkstationSolution> findByWorkstationProfileIdOrderByStartOrderAsc(UUID workstationProfileId);

    /**
     * Checks whether a solution profile is already assigned to a workstation profile.
     *
     * @param workstationProfileId the workstation profile ID
     * @param solutionProfileId    the solution profile ID
     * @return true if the solution is already in the workstation
     */
    boolean existsByWorkstationProfileIdAndSolutionProfileId(UUID workstationProfileId, UUID solutionProfileId);

    /**
     * Finds a specific solution assignment within a workstation.
     *
     * @param workstationProfileId the workstation profile ID
     * @param solutionProfileId    the solution profile ID
     * @return the workstation solution entry, if found
     */
    Optional<WorkstationSolution> findByWorkstationProfileIdAndSolutionProfileId(UUID workstationProfileId,
                                                                                  UUID solutionProfileId);

    /**
     * Counts the number of solutions in a workstation profile.
     *
     * @param workstationProfileId the workstation profile ID
     * @return the count of solutions
     */
    long countByWorkstationProfileId(UUID workstationProfileId);
}
