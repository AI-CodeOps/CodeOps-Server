package com.codeops.fleet.repository;

import com.codeops.fleet.entity.ContainerInstance;
import com.codeops.fleet.entity.enums.ContainerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ContainerInstance} entities.
 *
 * <p>Provides CRUD operations plus team-scoped queries for listing containers,
 * filtering by status and service name, and counting by status.</p>
 */
@Repository
public interface ContainerInstanceRepository extends JpaRepository<ContainerInstance, UUID> {

    /**
     * Finds all container instances belonging to a team.
     *
     * @param teamId the team ID
     * @return list of container instances
     */
    List<ContainerInstance> findByTeamId(UUID teamId);

    /**
     * Finds container instances for a team filtered by lifecycle status.
     *
     * @param teamId the team ID
     * @param status the container status filter
     * @return list of matching container instances
     */
    List<ContainerInstance> findByTeamIdAndStatus(UUID teamId, ContainerStatus status);

    /**
     * Finds container instances for a team filtered by service name.
     *
     * @param teamId      the team ID
     * @param serviceName the logical service name
     * @return list of matching container instances
     */
    List<ContainerInstance> findByTeamIdAndServiceName(UUID teamId, String serviceName);

    /**
     * Finds all container instances created from a specific service profile.
     *
     * @param serviceProfileId the service profile ID
     * @return list of container instances
     */
    List<ContainerInstance> findByServiceProfileId(UUID serviceProfileId);

    /**
     * Counts container instances for a team with a specific status.
     *
     * @param teamId the team ID
     * @param status the container status
     * @return count of matching containers
     */
    long countByTeamIdAndStatus(UUID teamId, ContainerStatus status);

    /**
     * Finds a container instance by team and container name.
     *
     * @param teamId        the team ID
     * @param containerName the Docker container name
     * @return the container instance, if found
     */
    Optional<ContainerInstance> findByTeamIdAndContainerName(UUID teamId, String containerName);
}
