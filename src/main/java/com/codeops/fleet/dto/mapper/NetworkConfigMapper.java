package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.NetworkConfigResponse;
import com.codeops.fleet.entity.NetworkConfig;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct mapper for NetworkConfig entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface NetworkConfigMapper {

    /**
     * Maps a network config entity to a response DTO.
     *
     * @param entity the network config entity
     * @return the response DTO
     */
    NetworkConfigResponse toResponse(NetworkConfig entity);

    /**
     * Maps a list of network config entities to response DTOs.
     *
     * @param entities the network config entities
     * @return the list of response DTOs
     */
    List<NetworkConfigResponse> toResponseList(List<NetworkConfig> entities);
}
