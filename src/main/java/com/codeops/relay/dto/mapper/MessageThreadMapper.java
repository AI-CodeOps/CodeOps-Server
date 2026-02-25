package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.MessageThreadResponse;
import com.codeops.relay.entity.MessageThread;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for MessageThread entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MessageThreadMapper {

    /**
     * Maps a MessageThread entity to a response DTO.
     *
     * @param entity the MessageThread entity
     * @return the response DTO
     */
    @Mapping(target = "participantIds", ignore = true)
    @Mapping(target = "replies", ignore = true)
    MessageThreadResponse toResponse(MessageThread entity);
}
