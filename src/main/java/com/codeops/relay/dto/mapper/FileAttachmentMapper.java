package com.codeops.relay.dto.mapper;

import com.codeops.relay.dto.response.FileAttachmentResponse;
import com.codeops.relay.entity.FileAttachment;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for FileAttachment entity to response DTO.
 */
@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface FileAttachmentMapper {

    /**
     * Maps a FileAttachment entity to a response DTO.
     *
     * @param entity the FileAttachment entity
     * @return the response DTO
     */
    @Mapping(target = "downloadUrl", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    FileAttachmentResponse toResponse(FileAttachment entity);
}
