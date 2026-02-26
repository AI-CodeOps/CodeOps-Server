package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.ContainerDetailResponse;
import com.codeops.fleet.dto.response.ContainerInstanceResponse;
import com.codeops.fleet.entity.ContainerInstance;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ContainerInstance entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ContainerInstanceMapper {

    /**
     * Maps a container instance to a lightweight list-view response.
     *
     * @param entity the container instance entity
     * @return the list-view response DTO
     */
    ContainerInstanceResponse toResponse(ContainerInstance entity);

    /**
     * Maps a container instance to a full detail response with relationships.
     *
     * @param entity the container instance entity
     * @return the detail response DTO
     */
    @Mapping(target = "serviceProfileId",
            expression = "java(entity.getServiceProfile() != null ? entity.getServiceProfile().getId() : null)")
    @Mapping(target = "serviceProfileName",
            expression = "java(entity.getServiceProfile() != null ? entity.getServiceProfile().getServiceName() : null)")
    @Mapping(source = "team.id", target = "teamId")
    ContainerDetailResponse toDetailResponse(ContainerInstance entity);

    /**
     * Maps a list of container instances to list-view responses.
     *
     * @param entities the container instance entities
     * @return the list of response DTOs
     */
    List<ContainerInstanceResponse> toResponseList(List<ContainerInstance> entities);
}
