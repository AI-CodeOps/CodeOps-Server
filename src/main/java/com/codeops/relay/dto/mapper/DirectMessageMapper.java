package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.request.SendDirectMessageRequest;
import com.codeops.relay.dto.response.DirectMessageResponse;
import com.codeops.relay.entity.DirectMessage;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for DirectMessage entity to/from DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DirectMessageMapper {

    /**
     * Maps a send request to a new DirectMessage entity.
     *
     * @param request the send direct message request
     * @return the DirectMessage entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "conversationId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "messageType", constant = "TEXT")
    @Mapping(target = "edited", constant = "false")
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "conversation", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    DirectMessage toEntity(SendDirectMessageRequest request);

    /**
     * Maps a DirectMessage entity to a response DTO.
     *
     * @param entity the DirectMessage entity
     * @return the response DTO
     */
    @Mapping(target = "senderDisplayName", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "isEdited", source = "edited")
    @Mapping(target = "isDeleted", source = "deleted")
    DirectMessageResponse toResponse(DirectMessage entity);
}
