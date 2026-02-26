package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.SolutionProfileDetailResponse;
import com.codeops.fleet.dto.response.SolutionProfileResponse;
import com.codeops.fleet.dto.response.SolutionServiceResponse;
import com.codeops.fleet.entity.SolutionProfile;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for SolutionProfile entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface SolutionProfileMapper {

    /**
     * Maps a solution profile to a lightweight list-view response.
     *
     * @param entity the solution profile entity
     * @return the list-view response DTO
     */
    @Mapping(target = "isDefault", source = "default")
    @Mapping(target = "teamId", source = "team.id")
    @Mapping(target = "serviceCount",
            expression = "java(entity.getServices() != null ? entity.getServices().size() : 0)")
    SolutionProfileResponse toResponse(SolutionProfile entity);

    /**
     * Maps a solution profile to a full detail response with services.
     *
     * @param entity   the solution profile entity
     * @param services the solution service responses
     * @return the detail response DTO
     */
    @Mapping(target = "isDefault", source = "entity.default")
    @Mapping(target = "teamId", source = "entity.team.id")
    @Mapping(source = "services", target = "services")
    SolutionProfileDetailResponse toDetailResponse(SolutionProfile entity,
                                                    List<SolutionServiceResponse> services);

    /**
     * Maps a list of solution profiles to list-view responses.
     *
     * @param entities the solution profile entities
     * @return the list of response DTOs
     */
    List<SolutionProfileResponse> toResponseList(List<SolutionProfile> entities);
}
