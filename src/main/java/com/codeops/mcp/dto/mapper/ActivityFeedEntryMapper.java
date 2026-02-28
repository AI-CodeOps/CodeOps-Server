package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.ActivityFeedEntryResponse;
import com.codeops.mcp.entity.ActivityFeedEntry;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ActivityFeedEntry entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ActivityFeedEntryMapper {

    /**
     * Maps an activity feed entry to a response DTO.
     *
     * @param entity the activity feed entry entity
     * @return the response DTO
     */
    @Mapping(target = "actorName",
            expression = "java(entity.getActor() != null ? entity.getActor().getDisplayName() : null)")
    @Mapping(target = "projectId",
            expression = "java(entity.getProject() != null ? entity.getProject().getId() : null)")
    @Mapping(target = "sessionId",
            expression = "java(entity.getSession() != null ? entity.getSession().getId() : null)")
    ActivityFeedEntryResponse toResponse(ActivityFeedEntry entity);

    /**
     * Maps a list of activity feed entries to response DTOs.
     *
     * @param entities the activity feed entry entities
     * @return the list of response DTOs
     */
    List<ActivityFeedEntryResponse> toResponseList(List<ActivityFeedEntry> entities);
}
