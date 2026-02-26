package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Team;
import com.codeops.fleet.entity.enums.ContainerStatus;
import com.codeops.fleet.entity.enums.HealthStatus;
import com.codeops.fleet.entity.enums.RestartPolicy;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Tracks a Docker container managed by Fleet.
 *
 * <p>Each container instance represents a running or previously running Docker container,
 * recording its lifecycle state, resource usage, health status, and the service profile
 * it was created from. Containers are team-scoped with unique names per team.</p>
 */
@Entity
@Table(name = "fleet_container_instances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fci_team_container_name",
                columnNames = {"team_id", "container_name"}),
        indexes = {
                @Index(name = "idx_fci_team_id", columnList = "team_id"),
                @Index(name = "idx_fci_status", columnList = "status"),
                @Index(name = "idx_fci_service_name", columnList = "service_name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContainerInstance extends BaseEntity {

    /** Docker container ID (64-char hex string, null before container creation). */
    @Column(name = "container_id", length = 64)
    private String containerId;

    /** Docker container name assigned at creation. */
    @Column(name = "container_name", nullable = false, length = 200)
    private String containerName;

    /** Logical service name matching Registry's service registration. */
    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    /** Docker image including repository path (e.g., {@code postgres:16}). */
    @Column(name = "image_name", nullable = false, length = 500)
    private String imageName;

    /** Docker image tag (e.g., {@code 16}, {@code latest}). */
    @Builder.Default
    @Column(name = "image_tag", length = 100)
    private String imageTag = "latest";

    /** Current lifecycle state of this container. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContainerStatus status = ContainerStatus.CREATED;

    /** Latest health check status for this container. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false)
    private HealthStatus healthStatus = HealthStatus.NONE;

    /** Active restart policy for this container. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "restart_policy", nullable = false)
    private RestartPolicy restartPolicy = RestartPolicy.NO;

    /** Number of times this container has been restarted. */
    @Builder.Default
    @Column(name = "restart_count", nullable = false)
    private Integer restartCount = 0;

    /** Last exit code returned by the container process (null if still running). */
    @Column(name = "exit_code")
    private Integer exitCode;

    /** Last sampled CPU usage percentage. */
    @Column(name = "cpu_percent")
    private Double cpuPercent;

    /** Last sampled memory usage in bytes. */
    @Column(name = "memory_bytes")
    private Long memoryBytes;

    /** Memory limit in bytes configured for this container. */
    @Column(name = "memory_limit_bytes")
    private Long memoryLimitBytes;

    /** Process ID of the container's main process. */
    @Column(name = "pid")
    private Integer pid;

    /** Timestamp when the container was last started. */
    @Column(name = "started_at")
    private Instant startedAt;

    /** Timestamp when the container last stopped or exited. */
    @Column(name = "finished_at")
    private Instant finishedAt;

    /** Last error message from the container runtime. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Service profile this container was created from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id")
    private ServiceProfile serviceProfile;

    /** Team that owns this container instance. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
}
