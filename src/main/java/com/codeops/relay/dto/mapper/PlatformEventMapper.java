package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.PlatformEventResponse;
import com.codeops.relay.entity.PlatformEvent;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for PlatformEvent entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PlatformEventMapper {

    /**
     * Maps a PlatformEvent entity to a response DTO.
     *
     * @param entity the PlatformEvent entity
     * @return the response DTO
     */
    @Mapping(target = "isDelivered", source = "delivered")
    PlatformEventResponse toResponse(PlatformEvent entity);
}
