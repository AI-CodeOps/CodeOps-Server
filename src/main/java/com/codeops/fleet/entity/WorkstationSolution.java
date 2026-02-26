package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity linking a {@link WorkstationProfile} to a {@link SolutionProfile} with ordering and overrides.
 *
 * <p>Each workstation solution entry represents one solution profile included in a workstation,
 * with a {@code startOrder} controlling the sequence in which solutions are started
 * (ascending) and stopped (descending). An optional {@code overrideEnvVarsJson} allows
 * per-workstation environment variable overrides for customization.</p>
 */
@Entity
@Table(name = "fleet_workstation_solutions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fws_workstation_solution",
                columnNames = {"workstation_profile_id", "solution_profile_id"}),
        indexes = {
                @Index(name = "idx_fws_workstation_id", columnList = "workstation_profile_id"),
                @Index(name = "idx_fws_solution_id", columnList = "solution_profile_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkstationSolution extends BaseEntity {

    /** Start priority order — lower values start first during orchestration. */
    @Builder.Default
    @Column(name = "start_order", nullable = false)
    private Integer startOrder = 0;

    /** JSON-encoded environment variable overrides for this workstation's use of the solution. */
    @Column(name = "override_env_vars_json", columnDefinition = "TEXT")
    private String overrideEnvVarsJson;

    /** The workstation profile this solution belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workstation_profile_id", nullable = false)
    private WorkstationProfile workstationProfile;

    /** The solution profile to be started as part of this workstation. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solution_profile_id", nullable = false)
    private SolutionProfile solutionProfile;
}
