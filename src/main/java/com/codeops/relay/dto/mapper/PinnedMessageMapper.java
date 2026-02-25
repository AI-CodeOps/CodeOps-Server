package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.PinnedMessageResponse;
import com.codeops.relay.entity.PinnedMessage;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PinnedMessage entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PinnedMessageMapper {

    /**
     * Maps a PinnedMessage entity to a response DTO.
     *
     * @param entity the PinnedMessage entity
     * @return the response DTO
     */
    @Mapping(target = "channelId", source = "channel.id")
    @Mapping(target = "message", ignore = true)
    PinnedMessageResponse toResponse(PinnedMessage entity);
}
