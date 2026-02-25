package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.request.SendMessageRequest;
import com.codeops.relay.dto.response.MessageResponse;
import com.codeops.relay.entity.Message;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Message entity to/from DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface MessageMapper {

    /**
     * Maps a send request to a new Message entity.
     *
     * @param request the send message request
     * @return the Message entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "channelId", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "messageType", constant = "TEXT")
    @Mapping(target = "edited", constant = "false")
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "platformEventId", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "mentionedUserIds", ignore = true)
    Message toEntity(SendMessageRequest request);

    /**
     * Maps a Message entity to a response DTO.
     *
     * @param entity the Message entity
     * @return the response DTO
     */
    @Mapping(target = "senderDisplayName", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "lastReplyAt", ignore = true)
    @Mapping(target = "mentionedUserIds", ignore = true)
    @Mapping(target = "isEdited", source = "edited")
    @Mapping(target = "isDeleted", source = "deleted")
    MessageResponse toResponse(Message entity);
}
