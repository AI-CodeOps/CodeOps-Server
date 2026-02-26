package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity between a {@link DeploymentRecord} and a {@link ContainerInstance}.
 *
 * <p>Records whether a specific container's participation in a deployment action
 * succeeded or failed, along with any error details. The combination of deployment
 * record and container is unique.</p>
 */
@Entity
@Table(name = "fleet_deployment_containers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fdc_deployment_container",
                columnNames = {"deployment_record_id", "container_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentContainer extends BaseEntity {

    /** Whether the deployment action succeeded for this container. */
    @Column(name = "success", nullable = false)
    private boolean success;

    /** Error message if the deployment action failed for this container. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Parent deployment record this container participated in. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deployment_record_id", nullable = false)
    private DeploymentRecord deploymentRecord;

    /** Container instance affected by the deployment action. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private ContainerInstance container;
}
