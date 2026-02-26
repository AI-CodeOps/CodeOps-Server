package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A developer's personal workspace configuration composed of one or more solutions.
 *
 * <p>A workstation profile defines which solution profiles a developer wants running
 * for their local development environment. Workstations are scoped to both a user and
 * a team, allowing per-user customization within the team context. Each user may have
 * at most one default workstation profile per name.</p>
 */
@Entity(name = "FleetWorkstationProfile")
@Table(name = "fleet_workstation_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fwp_user_name",
                columnNames = {"user_id", "name"}),
        indexes = {
                @Index(name = "idx_fwp_user_id", columnList = "user_id"),
                @Index(name = "idx_fwp_team_id", columnList = "team_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkstationProfile extends BaseEntity {

    /** Human-readable name of this workstation profile. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Optional description of what this workstation is for. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Whether this is the default workstation profile for this user. */
    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /** The user who owns this workstation profile. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Team context for this workstation profile. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** Solution profiles included in this workstation, with start ordering. */
    @Builder.Default
    @OneToMany(mappedBy = "workstationProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkstationSolution> solutions = new ArrayList<>();
}
