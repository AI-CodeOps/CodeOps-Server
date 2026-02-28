package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.McpSessionDetailResponse;
import com.codeops.mcp.dto.response.McpSessionResponse;
import com.codeops.mcp.dto.response.SessionResultResponse;
import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.McpSession;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for McpSession entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface McpSessionMapper {

    /**
     * Maps a session to a lightweight list-view response.
     *
     * @param entity the session entity
     * @return the list-view response DTO
     */
    @Mapping(source = "project.name", target = "projectName")
    @Mapping(source = "developerProfile.user.displayName", target = "developerName")
    McpSessionResponse toResponse(McpSession entity);

    /**
     * Maps a list of sessions to list-view responses.
     *
     * @param entities the session entities
     * @return the list of response DTOs
     */
    List<McpSessionResponse> toResponseList(List<McpSession> entities);

    /**
     * Maps a session to a detailed response with pre-built tool calls and result.
     *
     * @param entity    the session entity
     * @param toolCalls the pre-mapped tool call responses
     * @param result    the pre-mapped session result response
     * @return the detail response DTO
     */
    @Mapping(source = "entity.id", target = "id")
    @Mapping(source = "entity.createdAt", target = "createdAt")
    @Mapping(source = "entity.updatedAt", target = "updatedAt")
    @Mapping(source = "entity.project.name", target = "projectName")
    @Mapping(source = "entity.developerProfile.user.displayName", target = "developerName")
    @Mapping(source = "toolCalls", target = "toolCalls")
    @Mapping(source = "result", target = "result")
    McpSessionDetailResponse toDetailResponse(McpSession entity,
                                               List<SessionToolCallResponse> toolCalls,
                                               SessionResultResponse result);
}
