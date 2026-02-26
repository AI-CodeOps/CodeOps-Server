package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.fleet.entity.enums.HealthStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Historical health check result for a container instance.
 *
 * <p>Records the outcome of each health check execution, including the check output,
 * exit code, and execution duration. Used to track container health over time.</p>
 */
@Entity
@Table(name = "fleet_container_health_checks",
        indexes = @Index(name = "idx_fchc_container_id", columnList = "container_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerHealthCheck extends BaseEntity {

    /** Result status of this health check execution. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private HealthStatus status;

    /** Raw output produced by the health check command. */
    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    /** Exit code returned by the health check command. */
    @Column(name = "exit_code")
    private Integer exitCode;

    /** Duration of the health check execution in milliseconds. */
    @Column(name = "duration_ms")
    private Long durationMs;

    /** Container instance that was checked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private ContainerInstance container;
}
