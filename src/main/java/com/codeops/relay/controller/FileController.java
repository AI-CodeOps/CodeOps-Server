package com.codeops.relay.controller;

import com.codeops.config.AppConstants;
import com.codeops.relay.dto.response.FileAttachmentResponse;
import com.codeops.relay.service.FileAttachmentService;
import com.codeops.relay.service.FileAttachmentService.FileDownloadResult;
import com.codeops.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for file attachment operations in the Relay module.
 *
 * <p>Provides endpoints for uploading, downloading, listing, and deleting
 * file attachments associated with messages.</p>
 *
 * <p>All endpoints require authentication. File size and type validation
 * is performed in the service layer.</p>
 */
@RestController
@RequestMapping(AppConstants.RELAY_API_PREFIX + "/files")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
public class FileController {

    private final FileAttachmentService fileAttachmentService;

    /**
     * Uploads a file attachment for a message.
     *
     * @param messageId the message ID to attach the file to
     * @param file      the multipart file
     * @return the file attachment details
     * @throws IOException if the file cannot be read
     */
    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public FileAttachmentResponse uploadFile(
            @RequestParam UUID messageId,
            @RequestPart("file") MultipartFile file) throws IOException {
        UUID userId = SecurityUtils.getCurrentUserId();
        return fileAttachmentService.uploadFile(
                messageId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes(),
                userId);
    }

    /**
     * Downloads a file attachment.
     *
     * @param attachmentId the attachment ID
     * @return the file content with appropriate headers
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        FileDownloadResult result = fileAttachmentService.downloadFile(attachmentId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    /**
     * Lists all attachments for a message.
     *
     * @param messageId the message ID
     * @return the list of file attachments
     */
    @GetMapping("/messages/{messageId}")
    public List<FileAttachmentResponse> getAttachmentsForMessage(@PathVariable UUID messageId) {
        return fileAttachmentService.getAttachmentsForMessage(messageId);
    }

    /**
     * Deletes a file attachment.
     *
     * @param attachmentId the attachment ID
     */
    @DeleteMapping("/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        fileAttachmentService.deleteAttachment(attachmentId, userId);
    }
}
