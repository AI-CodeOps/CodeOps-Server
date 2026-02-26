package com.codeops.fleet.dto.mapper;

import com.codeops.fleet.dto.response.VolumeMountResponse;
import com.codeops.fleet.entity.VolumeMount;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for VolumeMount entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VolumeMountMapper {

    /**
     * Maps a volume mount entity to a response DTO.
     *
     * @param entity the volume mount entity
     * @return the response DTO
     */
    @Mapping(target = "isReadOnly", source = "readOnly")
    VolumeMountResponse toResponse(VolumeMount entity);

    /**
     * Maps a list of volume mount entities to response DTOs.
     *
     * @param entities the volume mount entities
     * @return the list of response DTOs
     */
    List<VolumeMountResponse> toResponseList(List<VolumeMount> entities);
}
