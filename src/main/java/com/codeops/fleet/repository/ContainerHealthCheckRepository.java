package com.codeops.fleet.repository;

import com.codeops.fleet.entity.ContainerHealthCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
