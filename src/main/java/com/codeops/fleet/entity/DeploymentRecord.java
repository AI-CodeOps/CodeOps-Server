package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.fleet.entity.enums.DeploymentAction;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Audit trail record of a container lifecycle action performed in Fleet.
 *
 * <p>Each deployment record captures who initiated the action, which team it belongs to,
 * how many services were affected, and the outcome (success/failure counts). The associated
 * {@link DeploymentContainer} entries detail the per-container results.</p>
 */
@Entity
@Table(name = "fleet_deployment_records",
        indexes = {
                @Index(name = "idx_fdr_team_id", columnList = "team_id"),
                @Index(name = "idx_fdr_triggered_by_id", columnList = "triggered_by_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentRecord extends BaseEntity {

    /** Type of deployment action that was performed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private DeploymentAction action;

    /** Human-readable description of this deployment action. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Total number of services affected by this deployment. */
    @Builder.Default
    @Column(name = "service_count", nullable = false)
    private Integer serviceCount = 0;

    /** Number of services that completed successfully. */
    @Builder.Default
    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    /** Number of services that failed during this deployment. */
    @Builder.Default
    @Column(name = "failure_count", nullable = false)
    private Integer failureCount = 0;

    /** Total time in milliseconds to complete the deployment. */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** User who initiated this deployment action. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id", nullable = false)
    private User triggeredBy;

    /** Team that owns this deployment record. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Container instances involved in this deployment with per-container results. */
    @Builder.Default
    @OneToMany(mappedBy = "deploymentRecord", cascade = CascadeType.ALL)
    private List<DeploymentContainer> containers = new ArrayList<>();
}
