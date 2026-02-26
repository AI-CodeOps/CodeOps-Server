package com.codeops.fleet.repository;

import com.codeops.fleet.entity.ContainerHealthCheck;
import com.codeops.fleet.entity.enums.HealthStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ContainerHealthCheck} entities.
 *
 * <p>Provides CRUD operations plus paginated health check history retrieval
 * and lookup of the most recent health check for a container.</p>
 */
@Repository
public interface ContainerHealthCheckRepository extends JpaRepository<ContainerHealthCheck, UUID> {

    /**
     * Finds health check results for a container with pagination.
     *
     * @param containerId the container instance ID
     * @param pageable    pagination parameters
     * @return a page of health check results
     */
    Page<ContainerHealthCheck> findByContainerId(UUID containerId, Pageable pageable);

    /**
     * Finds the most recent health check result for a container.
     *
     * @param containerId the container instance ID
     * @return the latest health check, if any exist
     */
    Optional<ContainerHealthCheck> findTopByContainerIdOrderByCreatedAtDesc(UUID containerId);

    /**
     * Counts health check records for a container with a specific status created after a given instant.
     *
     * <p>Used for crash loop detection — counts UNHEALTHY checks within a time window.</p>
     *
     * @param containerId the container instance ID
     * @param status      the health status to match
     * @param after       the earliest creation timestamp to include
     * @return count of matching health check records
     */
    long countByContainerIdAndStatusAndCreatedAtAfter(UUID containerId, HealthStatus status, Instant after);

    /**
     * Deletes all health check records for containers in a team that are older than a given instant.
     *
     * <p>Used for log retention — purges stale health check history.</p>
     *
     * @param teamId the team ID
     * @param before the cutoff timestamp; records created before this are deleted
     * @return the number of records deleted
     */
    @Modifying
    @Query("DELETE FROM ContainerHealthCheck c WHERE c.container.team.id = :teamId AND c.createdAt < :before")
    int deleteByTeamIdAndCreatedAtBefore(@Param("teamId") UUID teamId, @Param("before") Instant before);
}
