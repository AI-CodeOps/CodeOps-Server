package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Join entity linking a {@link SolutionProfile} to a {@link ServiceProfile} with start ordering.
 *
 * <p>Each solution service entry represents one service profile included in a solution,
 * with a {@code startOrder} controlling the sequence in which services are started
 * (ascending) and stopped (descending) during orchestrated operations.</p>
 */
@Entity
@Table(name = "fleet_solution_services",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fsls_solution_service",
                columnNames = {"solution_profile_id", "service_profile_id"}),
        indexes = {
                @Index(name = "idx_fsls_solution_id", columnList = "solution_profile_id"),
                @Index(name = "idx_fsls_service_id", columnList = "service_profile_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionService extends BaseEntity {

    /** Start priority order — lower values start first during orchestration. */
    @Builder.Default
    @Column(name = "start_order", nullable = false)
    private Integer startOrder = 0;

    /** The solution profile this service belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solution_profile_id", nullable = false)
    private SolutionProfile solutionProfile;

    /** The service profile to be started as part of this solution. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;
}
