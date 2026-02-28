package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.DeveloperProfileResponse;
import com.codeops.mcp.entity.DeveloperProfile;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for DeveloperProfile entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DeveloperProfileMapper {

    /**
     * Maps a developer profile to a response DTO.
     *
     * @param entity the developer profile entity
     * @return the response DTO
     */
    @Mapping(target = "isActive", source = "active")
    @Mapping(source = "team.id", target = "teamId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.displayName", target = "userDisplayName")
    DeveloperProfileResponse toResponse(DeveloperProfile entity);

    /**
     * Maps a list of developer profiles to response DTOs.
     *
     * @param entities the developer profile entities
     * @return the list of response DTOs
     */
    List<DeveloperProfileResponse> toResponseList(List<DeveloperProfile> entities);
}
