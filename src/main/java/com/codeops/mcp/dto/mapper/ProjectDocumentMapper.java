package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.ProjectDocument;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ProjectDocument entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProjectDocumentMapper {

    /**
     * Maps a project document to a lightweight list-view response.
     *
     * @param entity the project document entity
     * @return the list-view response DTO
     */
    @Mapping(target = "isFlagged", source = "flagged")
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(target = "lastUpdatedByName",
            expression = "java(entity.getLastUpdatedBy() != null ? entity.getLastUpdatedBy().getDisplayName() : null)")
    ProjectDocumentResponse toResponse(ProjectDocument entity);

    /**
     * Maps a list of project documents to list-view responses.
     *
     * @param entities the project document entities
     * @return the list of response DTOs
     */
    List<ProjectDocumentResponse> toResponseList(List<ProjectDocument> entities);

    /**
     * Maps a project document to a detailed response with pre-built version list.
     *
     * @param entity   the project document entity
     * @param versions the pre-mapped version responses
     * @return the detail response DTO
     */
    @Mapping(target = "isFlagged", source = "entity.flagged")
    @Mapping(source = "entity.project.id", target = "projectId")
    @Mapping(target = "lastUpdatedByName",
            expression = "java(entity.getLastUpdatedBy() != null ? entity.getLastUpdatedBy().getDisplayName() : null)")
    @Mapping(source = "versions", target = "versions")
    ProjectDocumentDetailResponse toDetailResponse(ProjectDocument entity,
                                                    List<ProjectDocumentVersionResponse> versions);
}
