package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.SessionToolCallResponse;
import com.codeops.mcp.entity.SessionToolCall;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for SessionToolCall entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SessionToolCallMapper {

    /**
     * Maps a tool call entity to a response DTO.
     *
     * @param entity the tool call entity
     * @return the response DTO
     */
    SessionToolCallResponse toResponse(SessionToolCall entity);

    /**
     * Maps a list of tool call entities to response DTOs.
     *
     * @param entities the tool call entities
     * @return the list of response DTOs
     */
    List<SessionToolCallResponse> toResponseList(List<SessionToolCall> entities);
}
