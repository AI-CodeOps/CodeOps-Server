package com.codeops.mcp.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for the result summary of a completed MCP session.
 *
 * @param id                    result ID
 * @param summary               AI-generated session summary
 * @param commitHashesJson      JSON array of commit hashes
 * @param filesChangedJson      JSON of files changed
 * @param endpointsChangedJson  JSON of endpoints changed
 * @param testsAdded            number of tests added
 * @param testCoverage          code coverage percentage
 * @param linesAdded            lines added
 * @param linesRemoved          lines removed
 * @param dependencyChangesJson JSON of dependency changes
 * @param durationMinutes       total session duration
 * @param tokenUsage            estimated token usage
 * @param createdAt             creation timestamp
 */
public record SessionResultResponse(
        UUID id,
        String summary,
        String commitHashesJson,
        String filesChangedJson,
        String endpointsChangedJson,
        int testsAdded,
        Double testCoverage,
        int linesAdded,
        int linesRemoved,
        String dependencyChangesJson,
        int durationMinutes,
        Long tokenUsage,
        Instant createdAt
) {}
