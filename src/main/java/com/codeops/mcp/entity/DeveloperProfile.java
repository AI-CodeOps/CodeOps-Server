package com.codeops.mcp.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.enums.Environment;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP developer profile linking a user to a team for AI-assisted development.
 *
 * <p>Stores developer-specific preferences and context used by AI agents
 * during MCP sessions. Each user has at most one profile per team.
 * Owns API tokens for MCP authentication and tracks session history.</p>
 */
@Entity
@Table(name = "mcp_developer_profiles",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mcp_dp_team_user",
                columnNames = {"team_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeveloperProfile extends BaseEntity {

    /** Override of user's name for MCP context. */
    @Column(name = "display_name", length = 200)
    private String displayName;

    /** Developer bio for AI context. */
    @Column(name = "bio", length = 2000)
    private String bio;

    /** Default deployment environment for new sessions. */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_environment")
    private Environment defaultEnvironment;

    /** JSON string containing personal AI preferences. */
    @Column(name = "preferences_json", columnDefinition = "TEXT")
    private String preferencesJson;

    /** IANA timezone identifier (e.g., "America/Chicago"). */
    @Column(name = "timezone", length = 50)
    private String timezone;

    /** Whether this developer profile is active. */
    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /** Team this developer profile belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    /** User this developer profile belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** API tokens owned by this developer profile. */
    @Builder.Default
    @OneToMany(mappedBy = "developerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<McpApiToken> tokens = new ArrayList<>();

    /** MCP sessions started by this developer profile. */
    @Builder.Default
    @OneToMany(mappedBy = "developerProfile")
    private List<McpSession> sessions = new ArrayList<>();
}
