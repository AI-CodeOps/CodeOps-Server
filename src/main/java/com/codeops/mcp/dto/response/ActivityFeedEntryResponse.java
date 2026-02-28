package com.codeops.mcp.dto.response;

import com.codeops.mcp.entity.enums.ActivityType;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an MCP activity feed entry.
 *
 * @param id                      entry ID
 * @param activityType            classification of the activity
 * @param title                   human-readable title
 * @param detail                  full detail or summary
 * @param sourceModule            originating CodeOps module
 * @param sourceEntityId          ID of the source entity
 * @param projectName             denormalized project name
 * @param impactedServiceIdsJson  JSON array of impacted service UUIDs
 * @param relayMessageId          Relay message ID if posted to channel
 * @param actorName               display name of the actor
 * @param projectId               associated project ID
 * @param sessionId               associated session ID
 * @param createdAt               creation timestamp
 */
public record ActivityFeedEntryResponse(
        UUID id,
        ActivityType activityType,
        String title,
        String detail,
        String sourceModule,
        UUID sourceEntityId,
        String projectName,
        String impactedServiceIdsJson,
        UUID relayMessageId,
        String actorName,
        UUID projectId,
        UUID sessionId,
        Instant createdAt
) {}
