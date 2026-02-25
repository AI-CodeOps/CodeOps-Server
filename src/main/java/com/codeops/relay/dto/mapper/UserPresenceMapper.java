package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.UserPresenceResponse;
import com.codeops.relay.entity.UserPresence;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for UserPresence entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface UserPresenceMapper {

    /**
     * Maps a UserPresence entity to a response DTO.
     *
     * @param entity the UserPresence entity
     * @return the response DTO
     */
    @Mapping(target = "userDisplayName", ignore = true)
    UserPresenceResponse toResponse(UserPresence entity);
}
