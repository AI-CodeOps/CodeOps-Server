package com.codeops.fleet.repository;

import com.codeops.fleet.entity.ContainerLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ContainerLog} entities.
 *
 * <p>Provides CRUD operations plus paginated log retrieval, time-range filtering,
 * and cleanup of old log entries for a container.</p>
 */
@Repository
public interface ContainerLogRepository extends JpaRepository<ContainerLog, UUID> {

    /**
     * Finds log entries for a container with pagination.
     *
     * @param containerId the container instance ID
     * @param pageable    pagination parameters
     * @return a page of log entries
     */
    Page<ContainerLog> findByContainerId(UUID containerId, Pageable pageable);

    /**
     * Finds log entries for a container within a time range with pagination.
     *
     * @param containerId the container instance ID
     * @param start       the start of the time range (inclusive)
     * @param end         the end of the time range (inclusive)
     * @param pageable    pagination parameters
     * @return a page of log entries within the time range
     */
    Page<ContainerLog> findByContainerIdAndTimestampBetween(
            UUID containerId, Instant start, Instant end, Pageable pageable);

    /**
     * Deletes log entries for a container that were created before a given timestamp.
     *
     * @param containerId the container instance ID
     * @param before      the cutoff timestamp
     */
    void deleteByContainerIdAndCreatedAtBefore(UUID containerId, Instant before);
}
