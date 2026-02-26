package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A named grouping of service profiles that together form a deployable solution.
 *
 * <p>A solution profile defines which service profiles (and their start order) compose
 * a logical application stack (e.g., "Backend API + Database + Cache"). Solutions are
 * team-scoped and can be marked as the default for quick one-click starts. Each team
 * may have at most one default solution profile.</p>
 */
@Entity
@Table(name = "fleet_solution_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fslp_team_name",
                columnNames = {"team_id", "name"}),
        indexes = {
                @Index(name = "idx_fslp_team_id", columnList = "team_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionProfile extends BaseEntity {

    /** Human-readable name of this solution profile. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Optional description of what this solution does. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Whether this is the default solution profile for the team. */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** Team that owns this solution profile. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Service profiles included in this solution, with start ordering. */
    @Builder.Default
    @OneToMany(mappedBy = "solutionProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolutionService> services = new ArrayList<>();
}
