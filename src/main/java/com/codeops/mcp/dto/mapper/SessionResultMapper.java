package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.SessionResultResponse;
import com.codeops.mcp.entity.SessionResult;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for SessionResult entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SessionResultMapper {

    /**
     * Maps a session result entity to a response DTO.
     *
     * @param entity the session result entity
     * @return the response DTO
     */
    SessionResultResponse toResponse(SessionResult entity);
}
