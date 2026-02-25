package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.ReactionResponse;
import com.codeops.relay.entity.Reaction;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Reaction entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ReactionMapper {

    /**
     * Maps a Reaction entity to a response DTO.
     *
     * @param entity the Reaction entity
     * @return the response DTO
     */
    @Mapping(target = "userDisplayName", ignore = true)
    ReactionResponse toResponse(Reaction entity);
}
