package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.ChannelMemberResponse;
import com.codeops.relay.entity.ChannelMember;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for ChannelMember entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ChannelMemberMapper {

    /**
     * Maps a ChannelMember entity to a response DTO.
     *
     * @param entity the ChannelMember entity
     * @return the response DTO
     */
    @Mapping(target = "userDisplayName", ignore = true)
    @Mapping(target = "isMuted", source = "muted")
    ChannelMemberResponse toResponse(ChannelMember entity);
}
