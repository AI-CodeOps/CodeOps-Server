package com.codeops.fleet.repository;

import com.codeops.fleet.entity.DeploymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DeploymentRecord} entities.
 *
 * <p>Provides CRUD operations plus paginated queries for deployment history
 * scoped by team or by the user who triggered the deployment.</p>
 */
@Repository
public interface DeploymentRecordRepository extends JpaRepository<DeploymentRecord, UUID> {

    /**
     * Finds deployment records for a team with pagination.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return a page of deployment records
     */
    Page<DeploymentRecord> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Finds deployment records triggered by a specific user with pagination.
     *
     * @param triggeredById the user ID who triggered the deployments
     * @param pageable      pagination parameters
     * @return a page of deployment records
     */
    Page<DeploymentRecord> findByTriggeredById(UUID triggeredById, Pageable pageable);
}
