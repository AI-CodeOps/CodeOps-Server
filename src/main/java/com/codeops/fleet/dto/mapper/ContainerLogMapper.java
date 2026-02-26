package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.ContainerLogResponse;
import com.codeops.fleet.entity.ContainerLog;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ContainerLog entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContainerLogMapper {

    /**
     * Maps a container log entity to a response DTO.
     *
     * @param entity the container log entity
     * @return the response DTO
     */
    @Mapping(source = "container.id", target = "containerId")
    ContainerLogResponse toResponse(ContainerLog entity);

    /**
     * Maps a list of container log entities to response DTOs.
     *
     * @param entities the container log entities
     * @return the list of response DTOs
     */
    List<ContainerLogResponse> toResponseList(List<ContainerLog> entities);
}
