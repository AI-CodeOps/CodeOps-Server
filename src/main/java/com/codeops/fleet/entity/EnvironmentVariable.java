package com.codeops.fleet.entity;

import com.codeops.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Environment variable configured for a service profile.
 *
 * <p>Stores key-value pairs that are injected into Docker containers at startup.
 * Variables can be marked as secrets to prevent display in the UI.</p>
 */
@Entity(name = "FleetEnvironmentVariable")
@Table(name = "fleet_environment_variables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentVariable extends BaseEntity {

    /** Environment variable name (e.g., {@code DATABASE_URL}, {@code SPRING_PROFILES_ACTIVE}). */
    @Column(name = "variable_key", nullable = false, length = 200)
    private String variableKey;

    /** Environment variable value. */
    @Column(name = "variable_value", nullable = false, columnDefinition = "TEXT")
    private String variableValue;

    /** Whether this variable contains a secret value that should be masked in the UI. */
    @Builder.Default
    @Column(name = "is_secret", nullable = false)
    private boolean isSecret = false;

    /** Parent service profile that owns this environment variable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_profile_id", nullable = false)
    private ServiceProfile serviceProfile;
}
