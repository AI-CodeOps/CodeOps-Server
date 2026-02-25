package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.DirectConversationResponse;
import com.codeops.relay.dto.response.DirectConversationSummaryResponse;
import com.codeops.relay.entity.DirectConversation;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for DirectConversation entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface DirectConversationMapper {

    /**
     * Maps a DirectConversation entity to a full response DTO.
     *
     * @param entity the DirectConversation entity
     * @return the response DTO
     */
    @Mapping(target = "participantIds", ignore = true)
    DirectConversationResponse toResponse(DirectConversation entity);

    /**
     * Maps a DirectConversation entity to a summary response DTO.
     *
     * @param entity the DirectConversation entity
     * @return the summary response DTO
     */
    @Mapping(target = "participantIds", ignore = true)
    @Mapping(target = "participantDisplayNames", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    DirectConversationSummaryResponse toSummaryResponse(DirectConversation entity);
}
