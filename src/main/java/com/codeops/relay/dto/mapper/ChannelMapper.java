package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.request.CreateChannelRequest;
import com.codeops.relay.dto.response.ChannelResponse;
import com.codeops.relay.dto.response.ChannelSummaryResponse;
import com.codeops.relay.entity.Channel;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Channel entity to/from DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ChannelMapper {

    /**
     * Maps a create request to a new Channel entity.
     *
     * @param request the create request
     * @return the Channel entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "teamId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "serviceId", ignore = true)
    @Mapping(target = "archived", constant = "false")
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "pinnedMessages", ignore = true)
    Channel toEntity(CreateChannelRequest request);

    /**
     * Maps a Channel entity to a full response DTO.
     *
     * @param entity the Channel entity
     * @return the response DTO
     */
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "isArchived", source = "archived")
    ChannelResponse toResponse(Channel entity);

    /**
     * Maps a Channel entity to a summary response DTO.
     *
     * @param entity the Channel entity
     * @return the summary response DTO
     */
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "lastMessageAt", ignore = true)
    @Mapping(target = "isArchived", source = "archived")
    ChannelSummaryResponse toSummaryResponse(Channel entity);
}
