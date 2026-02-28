package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.ApiTokenResponse;
import com.codeops.mcp.entity.McpApiToken;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for McpApiToken entity to response DTOs.
 *
 * <p>Note: The tokenHash field is deliberately excluded from the response
 * DTO for security. The ApiTokenCreatedResponse (which includes rawToken)
 * is built manually in the service layer since the raw token is never
 * stored in the entity.</p>
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface McpApiTokenMapper {

    /**
     * Maps a token entity to a response DTO (excludes tokenHash).
     *
     * @param entity the token entity
     * @return the response DTO
     */
    ApiTokenResponse toResponse(McpApiToken entity);

    /**
     * Maps a list of token entities to response DTOs.
     *
     * @param entities the token entities
     * @return the list of response DTOs
     */
    List<ApiTokenResponse> toResponseList(List<McpApiToken> entities);
}
