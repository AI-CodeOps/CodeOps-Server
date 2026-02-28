package com.codeops.mcp.service;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.dto.mapper.ActivityFeedEntryMapper;
import com.codeops.mcp.dto.response.ActivityFeedEntryResponse;
import com.codeops.mcp.entity.ActivityFeedEntry;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.SessionResult;
import com.codeops.mcp.entity.enums.ActivityType;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.service.PlatformEventService;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.UserRepository;
import com.codeops.mcp.repository.ActivityFeedEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Publishes and queries the MCP team activity feed.
 *
 * <p>Records significant MCP events — session completions, document updates,
 * convention changes, and cross-project impact detections — as feed entries.
 * Session completion events are also forwarded to Relay via
 * {@link PlatformEventService} for automatic project channel notifications.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityFeedService {

    private final ActivityFeedEntryRepository feedRepository;
    private final ActivityFeedEntryMapper feedMapper;
    private final PlatformEventService platformEventService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a session-completed activity entry and fires a Relay event.
     *
     * <p>Builds a title summarizing the session results including files changed,
     * tests added, and commit hashes. Also calls {@link PlatformEventService}
     * to post a notification to the project's Relay channel.</p>
     *
     * @param session the completed MCP session
     * @param result  the session result with metrics
     * @return the activity feed entry response
     */
    @Transactional
    public ActivityFeedEntryResponse publishSessionCompleted(McpSession session, SessionResult result) {
        String developerName = resolveDeveloperName(session);
        String projectName = session.getProject().getName();
        int totalFiles = result.getLinesAdded() + result.getLinesRemoved();

        String title = String.format("%s completed session on %s: %d lines changed, %d tests added",
                developerName, projectName, totalFiles, result.getTestsAdded());

        String detail = result.getSummary();

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .activityType(ActivityType.SESSION_COMPLETED)
                .title(title)
                .detail(detail)
                .sourceModule("mcp")
                .sourceEntityId(session.getId())
                .projectName(projectName)
                .team(session.getProject().getTeam())
                .actor(session.getDeveloperProfile().getUser())
                .project(session.getProject())
                .session(session)
                .build();

        entry = feedRepository.save(entry);

        // Fire Relay event for project channel notification
        try {
            UUID actorId = session.getDeveloperProfile().getUser().getId();
            UUID teamId = session.getProject().getTeam().getId();
            platformEventService.publishEvent(
                    PlatformEventType.SESSION_COMPLETED,
                    teamId,
                    session.getId(),
                    "mcp",
                    title,
                    detail,
                    actorId,
                    null);
        } catch (Exception e) {
            log.warn("Failed to publish Relay event for session {}: {}",
                    session.getId(), e.getMessage());
        }

        log.info("Published SESSION_COMPLETED activity for session {}", session.getId());
        return feedMapper.toResponse(entry);
    }

    /**
     * Publishes a session-failed activity entry.
     *
     * @param session      the failed MCP session
     * @param errorMessage the error that caused the failure
     * @return the activity feed entry response
     */
    @Transactional
    public ActivityFeedEntryResponse publishSessionFailed(McpSession session, String errorMessage) {
        String developerName = resolveDeveloperName(session);
        String projectName = session.getProject().getName();

        String title = String.format("%s session failed on %s", developerName, projectName);

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .activityType(ActivityType.SESSION_FAILED)
                .title(title)
                .detail(errorMessage)
                .sourceModule("mcp")
                .sourceEntityId(session.getId())
                .projectName(projectName)
                .team(session.getProject().getTeam())
                .actor(session.getDeveloperProfile().getUser())
                .project(session.getProject())
                .session(session)
                .build();

        entry = feedRepository.save(entry);

        log.info("Published SESSION_FAILED activity for session {}", session.getId());
        return feedMapper.toResponse(entry);
    }

    /**
     * Publishes a document-updated activity entry.
     *
     * @param teamId            the team ID
     * @param projectId         the project ID
     * @param actorId           the user who performed the update
     * @param docType           the document type name
     * @param changeDescription description of the change
     * @return the activity feed entry response
     */
    @Transactional
    public ActivityFeedEntryResponse publishDocumentUpdated(UUID teamId, UUID projectId,
                                                             UUID actorId, String docType,
                                                             String changeDescription) {
        Project project = projectRepository.findById(projectId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        Team team = project != null ? project.getTeam() : null;
        String projectName = project != null ? project.getName() : "Unknown";
        String actorName = actor != null ? actor.getDisplayName() : "Unknown";

        String title = String.format("%s updated %s on %s", actorName, docType, projectName);

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .activityType(ActivityType.DOCUMENT_UPDATED)
                .title(title)
                .detail(changeDescription)
                .sourceModule("mcp")
                .projectName(projectName)
                .team(team)
                .actor(actor)
                .project(project)
                .build();

        entry = feedRepository.save(entry);

        log.info("Published DOCUMENT_UPDATED activity for {} on project {}", docType, projectId);
        return feedMapper.toResponse(entry);
    }

    /**
     * Publishes a convention-changed activity entry.
     *
     * @param teamId         the team ID
     * @param actorId        the user who changed the convention
     * @param conventionName the convention or directive name
     * @return the activity feed entry response
     */
    @Transactional
    public ActivityFeedEntryResponse publishConventionChanged(UUID teamId, UUID actorId,
                                                               String conventionName) {
        User actor = userRepository.findById(actorId).orElse(null);
        String actorName = actor != null ? actor.getDisplayName() : "Unknown";

        String title = String.format("%s updated convention: %s", actorName, conventionName);

        Team team = new Team();
        team.setId(teamId);

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .activityType(ActivityType.CONVENTION_CHANGED)
                .title(title)
                .sourceModule("mcp")
                .team(team)
                .actor(actor)
                .build();

        entry = feedRepository.save(entry);

        log.info("Published CONVENTION_CHANGED activity for convention '{}' in team {}",
                conventionName, teamId);
        return feedMapper.toResponse(entry);
    }

    /**
     * Publishes a cross-project impact detection entry.
     *
     * <p>Stores the impacted service IDs as a JSON array for downstream
     * consumers to query.</p>
     *
     * @param teamId            the team ID
     * @param projectId         the source project
     * @param title             human-readable title describing the impact
     * @param detail            detailed description of the impact
     * @param impactedServiceIds list of service IDs affected by the change
     * @return the activity feed entry response
     */
    @Transactional
    public ActivityFeedEntryResponse publishImpactDetected(UUID teamId, UUID projectId,
                                                            String title, String detail,
                                                            List<UUID> impactedServiceIds) {
        Project project = projectRepository.findById(projectId).orElse(null);
        Team team = new Team();
        team.setId(teamId);
        String projectName = project != null ? project.getName() : "Unknown";

        String impactedJson = serializeJson(impactedServiceIds);

        ActivityFeedEntry entry = ActivityFeedEntry.builder()
                .activityType(ActivityType.IMPACT_DETECTED)
                .title(title)
                .detail(detail)
                .sourceModule("mcp")
                .projectName(projectName)
                .impactedServiceIdsJson(impactedJson)
                .team(team)
                .project(project)
                .build();

        entry = feedRepository.save(entry);

        log.info("Published IMPACT_DETECTED activity for project {} with {} impacted services",
                projectId, impactedServiceIds != null ? impactedServiceIds.size() : 0);
        return feedMapper.toResponse(entry);
    }

    /**
     * Retrieves the team activity feed with pagination.
     *
     * @param teamId   the team ID
     * @param pageable pagination parameters
     * @return page of activity feed entries, newest first
     */
    @Transactional(readOnly = true)
    public Page<ActivityFeedEntryResponse> getTeamFeed(UUID teamId, Pageable pageable) {
        Page<ActivityFeedEntry> entries = feedRepository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable);
        return entries.map(feedMapper::toResponse);
    }

    /**
     * Retrieves the project activity feed with pagination.
     *
     * @param projectId the project ID
     * @param pageable  pagination parameters
     * @return page of activity feed entries, newest first
     */
    @Transactional(readOnly = true)
    public Page<ActivityFeedEntryResponse> getProjectFeed(UUID projectId, Pageable pageable) {
        Page<ActivityFeedEntry> entries = feedRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
        return entries.map(feedMapper::toResponse);
    }

    /**
     * Retrieves team activity since a given timestamp.
     *
     * <p>Used by the MCP tool {@code codeops.getTeamActivity} for context assembly.</p>
     *
     * @param teamId the team ID
     * @param since  cutoff timestamp (entries after this time are returned)
     * @return list of activity entries, newest first
     */
    @Transactional(readOnly = true)
    public List<ActivityFeedEntryResponse> getTeamActivitySince(UUID teamId, Instant since) {
        List<ActivityFeedEntry> entries = feedRepository
                .findByTeamIdAndCreatedAtAfterOrderByCreatedAtDesc(teamId, since);
        return feedMapper.toResponseList(entries);
    }

    /**
     * Retrieves team activity filtered by type.
     *
     * @param teamId       the team ID
     * @param activityType the activity type to filter by
     * @return list of matching activity entries, newest first
     */
    @Transactional(readOnly = true)
    public List<ActivityFeedEntryResponse> getTeamActivityByType(UUID teamId, ActivityType activityType) {
        List<ActivityFeedEntry> entries = feedRepository
                .findByTeamIdAndActivityTypeOrderByCreatedAtDesc(teamId, activityType);
        return feedMapper.toResponseList(entries);
    }

    // ── Private Helpers ──

    /**
     * Resolves the developer display name from a session's profile.
     */
    private String resolveDeveloperName(McpSession session) {
        if (session.getDeveloperProfile() != null
                && session.getDeveloperProfile().getUser() != null) {
            return session.getDeveloperProfile().getUser().getDisplayName();
        }
        return "Unknown";
    }

    /**
     * Serializes an object to JSON, returning null on failure.
     */
    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize value to JSON", e);
            return null;
        }
    }
}
