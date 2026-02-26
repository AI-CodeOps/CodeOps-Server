package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.ContainerHealthCheckResponse;
import com.codeops.fleet.entity.ContainerHealthCheck;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ContainerHealthCheck entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContainerHealthCheckMapper {

    /**
     * Maps a health check entity to a response DTO.
     *
     * @param entity the container health check entity
     * @return the response DTO
     */
    @Mapping(source = "container.id", target = "containerId")
    ContainerHealthCheckResponse toResponse(ContainerHealthCheck entity);

    /**
     * Maps a list of health check entities to response DTOs.
     *
     * @param entities the health check entities
     * @return the list of response DTOs
     */
    List<ContainerHealthCheckResponse> toResponseList(List<ContainerHealthCheck> entities);
}
