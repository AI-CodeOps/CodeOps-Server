package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.entity.WorkstationSolution;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for WorkstationSolution join entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface WorkstationSolutionMapper {

    /**
     * Maps a workstation solution entry to a response DTO with nested solution profile details.
     *
     * @param entity the workstation solution entity
     * @return the response DTO
     */
    @Mapping(target = "solutionProfileId", source = "solutionProfile.id")
    @Mapping(target = "solutionProfileName", source = "solutionProfile.name")
    WorkstationSolutionResponse toResponse(WorkstationSolution entity);

    /**
     * Maps a list of workstation solution entries to response DTOs.
     *
     * @param entities the workstation solution entities
     * @return the list of response DTOs
     */
    List<WorkstationSolutionResponse> toResponseList(List<WorkstationSolution> entities);
}
