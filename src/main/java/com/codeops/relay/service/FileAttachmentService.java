package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.FileAttachmentMapper;
import com.codeops.relay.dto.response.FileAttachmentResponse;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.FileAttachment;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.enums.FileUploadStatus;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.FileAttachmentRepository;
import com.codeops.relay.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing file attachments on channel messages.
 *
 * <p>Handles file uploads to local storage (S3-ready interface for prod),
 * downloads with channel membership verification, and attachment lifecycle
 * including per-message limits and bulk cleanup.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileAttachmentService {

    private final FileAttachmentRepository fileAttachmentRepository;
    private final MessageRepository messageRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final FileAttachmentMapper fileAttachmentMapper;

    @Value("${relay.file-storage.base-path:${user.home}/codeops-files}")
    private String fileStorageBasePath;

    @Value("${relay.file-storage.max-file-size:#{26214400}}")
    private long maxFileSize;

    /** Result record for file downloads. */
    public record FileDownloadResult(String fileName, String contentType, byte[] content) {}

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("zip", "application/zip"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("csv", "text/csv"),
            Map.entry("html", "text/html"),
            Map.entry("svg", "image/svg+xml")
    );

    /**
     * Uploads a file and attaches it to a channel message.
     *
     * <p>Validates file size, name, and per-message attachment limits. Writes the file
     * to local storage under {@code {basePath}/{channelId}/{messageId}/{uuid}_{sanitizedName}}.
     * Creates a {@link FileAttachment} entity with status {@code COMPLETE}.</p>
     *
     * @param messageId   the channel message ID
     * @param fileName    the original file name
     * @param contentType the MIME type (detected from extension if null)
     * @param fileContent the file bytes
     * @param userId      the uploading user's ID
     * @return the created FileAttachmentResponse
     * @throws NotFoundException      if the message does not exist
     * @throws AuthorizationException if the user is not a member of the message's channel
     * @throws ValidationException    if the file is empty, too large, name is blank, or attachment limit exceeded
     */
    @Transactional
    public FileAttachmentResponse uploadFile(UUID messageId, String fileName, String contentType,
                                              byte[] fileContent, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException("Message", messageId));
        verifyChannelMember(message.getChannelId(), userId);

        if (fileContent == null || fileContent.length == 0) {
            throw new ValidationException("File content must not be empty");
        }
        if (fileContent.length > maxFileSize) {
            throw new ValidationException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new ValidationException("File name must not be blank");
        }

        long currentCount = fileAttachmentRepository.countByMessageId(messageId);
        if (currentCount >= AppConstants.RELAY_MAX_ATTACHMENTS_PER_MESSAGE) {
            throw new ValidationException("Maximum of " + AppConstants.RELAY_MAX_ATTACHMENTS_PER_MESSAGE
                    + " attachments per message exceeded");
        }

        String sanitized = sanitizeFileName(fileName);
        String resolvedContentType = contentType != null && !contentType.isBlank()
                ? contentType : detectMimeType(fileName);

        String storagePath = fileStorageBasePath + "/" + message.getChannelId() + "/"
                + messageId + "/" + UUID.randomUUID() + "_" + sanitized;

        writeFileToDisk(storagePath, fileContent);

        FileAttachment attachment = FileAttachment.builder()
                .messageId(messageId)
                .uploadedBy(userId)
                .teamId(message.getChannelId())
                .fileName(fileName)
                .fileSizeBytes((long) fileContent.length)
                .contentType(resolvedContentType)
                .storagePath(storagePath)
                .status(FileUploadStatus.COMPLETE)
                .build();
        attachment = fileAttachmentRepository.save(attachment);

        log.info("File uploaded: {} ({} bytes) to message {}", fileName, fileContent.length, messageId);
        return buildFileAttachmentResponse(attachment);
    }

    /**
     * Downloads a file attachment.
     *
     * <p>Verifies that the requesting user is a member of the channel the message
     * belongs to, then reads the file from local storage.</p>
     *
     * @param attachmentId the file attachment ID
     * @param userId       the requesting user's ID
     * @return the file download result containing name, content type, and bytes
     * @throws NotFoundException      if the attachment or its message does not exist
     * @throws AuthorizationException if the user is not a member of the message's channel
     * @throws RuntimeException       if the file is missing from disk
     */
    @Transactional(readOnly = true)
    public FileDownloadResult downloadFile(UUID attachmentId, UUID userId) {
        FileAttachment attachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("File attachment", attachmentId));

        Message message = messageRepository.findById(attachment.getMessageId())
                .orElseThrow(() -> new NotFoundException("Message", attachment.getMessageId()));
        verifyChannelMember(message.getChannelId(), userId);

        byte[] content = readFileFromDisk(attachment.getStoragePath());
        return new FileDownloadResult(attachment.getFileName(), attachment.getContentType(), content);
    }

    /**
     * Retrieves all attachments for a message, ordered by creation time.
     *
     * @param messageId the channel message ID
     * @return list of file attachment responses with download URLs
     */
    @Transactional(readOnly = true)
    public List<FileAttachmentResponse> getAttachmentsForMessage(UUID messageId) {
        return fileAttachmentRepository.findByMessageIdOrderByCreatedAtAsc(messageId).stream()
                .map(this::buildFileAttachmentResponse)
                .toList();
    }

    /**
     * Deletes a file attachment.
     *
     * <p>Authorized for the uploader or channel OWNER/ADMIN. Removes the file
     * from disk (best effort) and deletes the entity.</p>
     *
     * @param attachmentId the file attachment ID
     * @param userId       the requesting user's ID
     * @throws NotFoundException      if the attachment does not exist
     * @throws AuthorizationException if the user is not the uploader or a channel admin/owner
     */
    @Transactional
    public void deleteAttachment(UUID attachmentId, UUID userId) {
        FileAttachment attachment = fileAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("File attachment", attachmentId));

        if (!attachment.getUploadedBy().equals(userId)) {
            Message message = messageRepository.findById(attachment.getMessageId())
                    .orElseThrow(() -> new NotFoundException("Message", attachment.getMessageId()));
            ChannelMember member = channelMemberRepository.findByChannelIdAndUserId(
                    message.getChannelId(), userId)
                    .orElseThrow(() -> new AuthorizationException("User is not a member of this channel"));
            if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
                throw new AuthorizationException("Only the uploader or channel admin/owner can delete attachments");
            }
        }

        deleteFileFromDisk(attachment.getStoragePath());
        fileAttachmentRepository.delete(attachment);
        log.info("File attachment {} deleted by user {}", attachmentId, userId);
    }

    /**
     * Deletes all attachments for a message (bulk cleanup).
     *
     * <p>Removes files from disk (best effort) and deletes all entities.
     * Used internally when a message is hard-deleted.</p>
     *
     * @param messageId the channel message ID
     */
    @Transactional
    public void deleteAllAttachmentsForMessage(UUID messageId) {
        List<FileAttachment> attachments = fileAttachmentRepository.findByMessageId(messageId);
        for (FileAttachment attachment : attachments) {
            deleteFileFromDisk(attachment.getStoragePath());
        }
        fileAttachmentRepository.deleteAll(attachments);
        log.debug("All attachments removed for message {}", messageId);
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Sanitizes a file name by replacing non-alphanumeric characters (except dot,
     * hyphen, and underscore) with underscores, then collapsing multiple underscores.
     *
     * @param fileName the original file name
     * @return the sanitized file name
     */
    static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");
    }

    /**
     * Generates a download URL for a file attachment.
     *
     * @param attachmentId the file attachment ID
     * @return the download URL path
     */
    private String generateDownloadUrl(UUID attachmentId) {
        return "/api/v1/relay/files/" + attachmentId + "/download";
    }

    /**
     * Detects the MIME type from a file name's extension.
     *
     * @param fileName the file name
     * @return the detected MIME type, or "application/octet-stream" if unknown
     */
    static String detectMimeType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "application/octet-stream";
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    /**
     * Verifies that a user is a member of the specified channel.
     *
     * @param channelId the channel ID
     * @param userId    the user ID
     * @throws AuthorizationException if the user is not a channel member
     */
    private void verifyChannelMember(UUID channelId, UUID userId) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channelId, userId)) {
            throw new AuthorizationException("User is not a member of this channel");
        }
    }

    /**
     * Builds a FileAttachmentResponse from a FileAttachment entity.
     *
     * @param attachment the file attachment entity
     * @return the response DTO with download URL
     */
    private FileAttachmentResponse buildFileAttachmentResponse(FileAttachment attachment) {
        return new FileAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSizeBytes(),
                generateDownloadUrl(attachment.getId()),
                attachment.getThumbnailPath(),
                attachment.getStatus(),
                attachment.getUploadedBy(),
                attachment.getCreatedAt());
    }

    /**
     * Writes file bytes to local disk storage.
     *
     * @param storagePath the full storage path
     * @param content     the file bytes
     * @throws RuntimeException if the write fails
     */
    private void writeFileToDisk(String storagePath, byte[] content) {
        try {
            Path path = Paths.get(storagePath);
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file to disk: " + storagePath, e);
        }
    }

    /**
     * Reads file bytes from local disk storage.
     *
     * @param storagePath the full storage path
     * @return the file bytes
     * @throws RuntimeException if the file is missing or read fails
     */
    private byte[] readFileFromDisk(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (!Files.exists(path)) {
                throw new RuntimeException("File not found on disk: " + storagePath);
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from disk: " + storagePath, e);
        }
    }

    /**
     * Deletes a file from local disk storage (best effort).
     *
     * @param storagePath the full storage path
     */
    private void deleteFileFromDisk(String storagePath) {
        try {
            Path path = Paths.get(storagePath);
            if (Files.exists(path)) {
                Files.delete(path);
            } else {
                log.warn("File not found on disk during deletion: {}", storagePath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", storagePath, e);
        }
    }
}
