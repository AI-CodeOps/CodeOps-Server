package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.entity.SolutionService;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for SolutionService join entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SolutionServiceMapper {

    /**
     * Maps a solution service entry to a response DTO with nested service profile details.
     *
     * @param entity the solution service entity
     * @return the response DTO
     */
    @Mapping(target = "serviceProfileId", source = "serviceProfile.id")
    @Mapping(target = "serviceProfileName", source = "serviceProfile.serviceName")
    @Mapping(target = "imageName", source = "serviceProfile.imageName")
    @Mapping(target = "isEnabled", source = "serviceProfile.enabled")
    SolutionServiceResponse toResponse(SolutionService entity);

    /**
     * Maps a list of solution service entries to response DTOs.
     *
     * @param entities the solution service entities
     * @return the list of response DTOs
     */
    List<SolutionServiceResponse> toResponseList(List<SolutionService> entities);
}
