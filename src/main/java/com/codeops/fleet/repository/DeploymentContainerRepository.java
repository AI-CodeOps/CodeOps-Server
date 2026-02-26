package com.codeops.fleet.repository;

import com.codeops.fleet.entity.DeploymentContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link DeploymentContainer} entities.
 *
 * <p>Provides CRUD operations plus queries to find deployment container entries
 * by deployment record or by container instance.</p>
 */
@Repository
public interface DeploymentContainerRepository extends JpaRepository<DeploymentContainer, UUID> {

    /**
     * Finds all deployment container entries for a deployment record.
     *
     * @param deploymentRecordId the deployment record ID
     * @return list of deployment container entries
     */
    List<DeploymentContainer> findByDeploymentRecordId(UUID deploymentRecordId);

    /**
     * Finds all deployment container entries involving a specific container.
     *
     * @param containerId the container instance ID
     * @return list of deployment container entries
     */
    List<DeploymentContainer> findByContainerId(UUID containerId);
}
