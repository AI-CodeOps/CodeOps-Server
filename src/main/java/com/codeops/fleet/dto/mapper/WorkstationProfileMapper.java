package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.WorkstationProfileDetailResponse;
import com.codeops.fleet.dto.response.WorkstationProfileResponse;
import com.codeops.fleet.dto.response.WorkstationSolutionResponse;
import com.codeops.fleet.entity.WorkstationProfile;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for WorkstationProfile entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface WorkstationProfileMapper {

    /**
     * Maps a workstation profile to a lightweight list-view response.
     *
     * @param entity the workstation profile entity
     * @return the list-view response DTO
     */
    @Mapping(target = "isDefault", source = "default")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "solutionCount",
            expression = "java(entity.getSolutions() != null ? entity.getSolutions().size() : 0)")
    WorkstationProfileResponse toResponse(WorkstationProfile entity);

    /**
     * Maps a workstation profile to a full detail response with solutions.
     *
     * @param entity    the workstation profile entity
     * @param solutions the workstation solution responses
     * @return the detail response DTO
     */
    @Mapping(target = "isDefault", source = "entity.default")
    @Mapping(target = "userId", source = "entity.user.id")
    @Mapping(target = "teamId", source = "entity.team.id")
    @Mapping(source = "solutions", target = "solutions")
    WorkstationProfileDetailResponse toDetailResponse(WorkstationProfile entity,
                                                       List<WorkstationSolutionResponse> solutions);

    /**
     * Maps a list of workstation profiles to list-view responses.
     *
     * @param entities the workstation profile entities
     * @return the list of response DTOs
     */
    List<WorkstationProfileResponse> toResponseList(List<WorkstationProfile> entities);
}
