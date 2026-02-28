package com.codeops.mcp.dto.mapper;

import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.ProjectDocumentVersion;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ProjectDocumentVersion entity to response DTOs.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ProjectDocumentVersionMapper {

    /**
     * Maps a document version entity to a response DTO.
     *
     * @param entity the document version entity
     * @return the response DTO
     */
    @Mapping(target = "authorName",
            expression = "java(entity.getAuthor() != null ? entity.getAuthor().getDisplayName() : null)")
    @Mapping(target = "sessionId",
            expression = "java(entity.getSession() != null ? entity.getSession().getId() : null)")
    ProjectDocumentVersionResponse toResponse(ProjectDocumentVersion entity);

    /**
     * Maps a list of document version entities to response DTOs.
     *
     * @param entities the document version entities
     * @return the list of response DTOs
     */
    List<ProjectDocumentVersionResponse> toResponseList(List<ProjectDocumentVersion> entities);
}
