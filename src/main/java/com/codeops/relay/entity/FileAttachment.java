package com.codeops.relay.entity;

import com.codeops.entity.BaseEntity;
import com.codeops.relay.entity.enums.FileUploadStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Represents a file attached to a channel message or direct message.
 *
 * <p>Tracks upload lifecycle (UPLOADING → COMPLETE/FAILED), file metadata,
 * and storage location. A file is linked to either a {@link Message} or a
 * {@link DirectMessage}, but not both.</p>
 */
@Entity
@Table(name = "file_attachments",
        indexes = {
                @Index(name = "idx_file_attachment_message_id", columnList = "message_id"),
                @Index(name = "idx_file_attachment_dm_id", columnList = "direct_message_id"),
                @Index(name = "idx_file_attachment_team_id", columnList = "team_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment extends BaseEntity {

    /** Original file name as uploaded by the user. */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /** MIME type of the file (e.g., "image/png", "application/pdf"). */
    @Column(name = "content_type", nullable = false, length = 200)
    private String contentType;

    /** File size in bytes. */
    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    /** Local filesystem path or S3 key where the file is stored. */
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    /** Path to a thumbnail image (for image file types). */
    @Column(name = "thumbnail_path", length = 1000)
    private String thumbnailPath;

    /** Current upload status of this file. */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileUploadStatus status = FileUploadStatus.UPLOADING;

    /** User who uploaded this file. */
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    /** Team this file belongs to. */
    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    /** Channel message this file is attached to (null if on a direct message). */
    @Column(name = "message_id")
    private UUID messageId;

    /** Direct message this file is attached to (null if on a channel message). */
    @Column(name = "direct_message_id")
    private UUID directMessageId;

    /** The channel message entity (read-only relationship). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", insertable = false, updatable = false)
    private Message message;

    /** The direct message entity (read-only relationship). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direct_message_id", insertable = false, updatable = false)
    private DirectMessage directMessage;
}
