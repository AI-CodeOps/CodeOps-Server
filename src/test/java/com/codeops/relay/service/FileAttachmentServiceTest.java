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
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.FileAttachmentRepository;
import com.codeops.relay.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FileAttachmentService}.
 *
 * <p>Covers upload, download, retrieval, deletion, and helper methods.
 * Uses {@code @TempDir} for file I/O operations.</p>
 */
@ExtendWith(MockitoExtension.class)
class FileAttachmentServiceTest {

    @Mock private FileAttachmentRepository fileAttachmentRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private FileAttachmentMapper fileAttachmentMapper;

    @InjectMocks private FileAttachmentService fileAttachmentService;

    @TempDir
    Path tempDir;

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ATTACHMENT_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Message message;

    @BeforeEach
    void setUp() {
        message = Message.builder()
                .channelId(CHANNEL_ID)
                .senderId(USER_ID)
                .content("Hello")
                .messageType(MessageType.TEXT)
                .build();
        message.setId(MESSAGE_ID);
        message.setCreatedAt(NOW);
        message.setUpdatedAt(NOW);

        ReflectionTestUtils.setField(fileAttachmentService, "fileStorageBasePath", tempDir.toString());
        ReflectionTestUtils.setField(fileAttachmentService, "maxFileSize", 26214400L);
    }

    // ── Upload ───────────────────────────────────────────────────────────

    @Nested
    class UploadTests {

        @Test
        void uploadFile_success_writesToDisk() {
            byte[] content = "file content".getBytes();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(fileAttachmentRepository.countByMessageId(MESSAGE_ID)).thenReturn(0L);
            when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(inv -> {
                FileAttachment a = inv.getArgument(0);
                a.setId(ATTACHMENT_ID);
                a.setCreatedAt(NOW);
                return a;
            });

            FileAttachmentResponse result = fileAttachmentService.uploadFile(
                    MESSAGE_ID, "test.txt", "text/plain", content, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.fileName()).isEqualTo("test.txt");
            assertThat(result.contentType()).isEqualTo("text/plain");
            assertThat(result.fileSizeBytes()).isEqualTo(content.length);
            assertThat(result.downloadUrl()).contains("/api/v1/relay/files/");
            assertThat(result.status()).isEqualTo(FileUploadStatus.COMPLETE);

            ArgumentCaptor<FileAttachment> captor = ArgumentCaptor.forClass(FileAttachment.class);
            verify(fileAttachmentRepository).save(captor.capture());
            FileAttachment saved = captor.getValue();
            assertThat(saved.getStoragePath()).startsWith(tempDir.toString());
            assertThat(Files.exists(Path.of(saved.getStoragePath()))).isTrue();
        }

        @Test
        void uploadFile_tooLarge_throwsValidation() {
            ReflectionTestUtils.setField(fileAttachmentService, "maxFileSize", 10L);
            byte[] content = "this is too large".getBytes();
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(
                    MESSAGE_ID, "big.txt", null, content, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum allowed size");
        }

        @Test
        void uploadFile_emptyContent_throwsValidation() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(
                    MESSAGE_ID, "empty.txt", null, new byte[0], USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        void uploadFile_blankFileName_throwsValidation() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(
                    MESSAGE_ID, "   ", null, "data".getBytes(), USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        void uploadFile_maxAttachments_throwsValidation() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(fileAttachmentRepository.countByMessageId(MESSAGE_ID))
                    .thenReturn((long) AppConstants.RELAY_MAX_ATTACHMENTS_PER_MESSAGE);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(
                    MESSAGE_ID, "test.txt", null, "data".getBytes(), USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("attachments per message");
        }

        @Test
        void uploadFile_notChannelMember_throwsAuth() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileAttachmentService.uploadFile(
                    MESSAGE_ID, "test.txt", null, "data".getBytes(), USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void uploadFile_sanitizesFileName() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(fileAttachmentRepository.countByMessageId(MESSAGE_ID)).thenReturn(0L);
            when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(inv -> {
                FileAttachment a = inv.getArgument(0);
                a.setId(ATTACHMENT_ID);
                a.setCreatedAt(NOW);
                return a;
            });

            fileAttachmentService.uploadFile(
                    MESSAGE_ID, "my file (1).txt", null, "data".getBytes(), USER_ID);

            ArgumentCaptor<FileAttachment> captor = ArgumentCaptor.forClass(FileAttachment.class);
            verify(fileAttachmentRepository).save(captor.capture());
            assertThat(captor.getValue().getStoragePath()).contains("my_file_1_.txt");
        }

        @Test
        void uploadFile_detectsMimeType() {
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);
            when(fileAttachmentRepository.countByMessageId(MESSAGE_ID)).thenReturn(0L);
            when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(inv -> {
                FileAttachment a = inv.getArgument(0);
                a.setId(ATTACHMENT_ID);
                a.setCreatedAt(NOW);
                return a;
            });

            fileAttachmentService.uploadFile(
                    MESSAGE_ID, "photo.png", null, "data".getBytes(), USER_ID);

            ArgumentCaptor<FileAttachment> captor = ArgumentCaptor.forClass(FileAttachment.class);
            verify(fileAttachmentRepository).save(captor.capture());
            assertThat(captor.getValue().getContentType()).isEqualTo("image/png");
        }
    }

    // ── Download ─────────────────────────────────────────────────────────

    @Nested
    class DownloadTests {

        @Test
        void downloadFile_success() throws IOException {
            byte[] content = "hello world".getBytes();
            Path filePath = tempDir.resolve("download-test.txt");
            Files.write(filePath, content);

            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, filePath.toString());

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);

            FileAttachmentService.FileDownloadResult result = fileAttachmentService.downloadFile(
                    ATTACHMENT_ID, USER_ID);

            assertThat(result.fileName()).isEqualTo("test.txt");
            assertThat(result.contentType()).isEqualTo("text/plain");
            assertThat(result.content()).isEqualTo(content);
        }

        @Test
        void downloadFile_notChannelMember_throwsAuth() throws IOException {
            Path filePath = tempDir.resolve("auth-test.txt");
            Files.write(filePath, "data".getBytes());
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, filePath.toString());

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> fileAttachmentService.downloadFile(ATTACHMENT_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void downloadFile_notFound_throws() {
            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileAttachmentService.downloadFile(ATTACHMENT_ID, USER_ID))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void downloadFile_fileMissingOnDisk_throwsRuntime() {
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, "/nonexistent/path/file.txt");

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> fileAttachmentService.downloadFile(ATTACHMENT_ID, USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found on disk");
        }
    }

    // ── Get ──────────────────────────────────────────────────────────────

    @Nested
    class GetTests {

        @Test
        void getAttachmentsForMessage_orderedByCreatedAt() {
            FileAttachment a1 = buildAttachment(UUID.randomUUID(), "/path/a1");
            FileAttachment a2 = buildAttachment(UUID.randomUUID(), "/path/a2");
            a2.setFileName("second.txt");

            when(fileAttachmentRepository.findByMessageIdOrderByCreatedAtAsc(MESSAGE_ID))
                    .thenReturn(List.of(a1, a2));

            List<FileAttachmentResponse> result = fileAttachmentService.getAttachmentsForMessage(MESSAGE_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).fileName()).isEqualTo("test.txt");
            assertThat(result.get(1).fileName()).isEqualTo("second.txt");
        }

        @Test
        void getAttachmentsForMessage_generatesDownloadUrls() {
            UUID id = UUID.randomUUID();
            FileAttachment a = buildAttachment(id, "/path/a");

            when(fileAttachmentRepository.findByMessageIdOrderByCreatedAtAsc(MESSAGE_ID))
                    .thenReturn(List.of(a));

            List<FileAttachmentResponse> result = fileAttachmentService.getAttachmentsForMessage(MESSAGE_ID);

            assertThat(result.get(0).downloadUrl())
                    .isEqualTo("/api/v1/relay/files/" + id + "/download");
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────

    @Nested
    class DeleteTests {

        @Test
        void deleteAttachment_byUploader_success() throws IOException {
            Path filePath = tempDir.resolve("delete-test.txt");
            Files.write(filePath, "data".getBytes());
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, filePath.toString());

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));

            fileAttachmentService.deleteAttachment(ATTACHMENT_ID, USER_ID);

            verify(fileAttachmentRepository).delete(attachment);
            assertThat(Files.exists(filePath)).isFalse();
        }

        @Test
        void deleteAttachment_byChannelAdmin_success() throws IOException {
            UUID adminId = UUID.randomUUID();
            Path filePath = tempDir.resolve("admin-delete.txt");
            Files.write(filePath, "data".getBytes());
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, filePath.toString());

            ChannelMember adminMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(adminId).role(MemberRole.ADMIN).build();

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, adminId))
                    .thenReturn(Optional.of(adminMember));

            fileAttachmentService.deleteAttachment(ATTACHMENT_ID, adminId);

            verify(fileAttachmentRepository).delete(attachment);
        }

        @Test
        void deleteAttachment_byOtherUser_throwsAuth() {
            UUID otherId = UUID.randomUUID();
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, "/path/test.txt");

            ChannelMember regularMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(otherId).role(MemberRole.MEMBER).build();

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));
            when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.of(message));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, otherId))
                    .thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> fileAttachmentService.deleteAttachment(ATTACHMENT_ID, otherId))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("admin/owner");
        }

        @Test
        void deleteAttachment_fileMissingOnDisk_logsWarning() {
            FileAttachment attachment = buildAttachment(ATTACHMENT_ID, "/nonexistent/file.txt");

            when(fileAttachmentRepository.findById(ATTACHMENT_ID)).thenReturn(Optional.of(attachment));

            fileAttachmentService.deleteAttachment(ATTACHMENT_ID, USER_ID);

            verify(fileAttachmentRepository).delete(attachment);
        }

        @Test
        void deleteAllAttachmentsForMessage_success() throws IOException {
            Path f1 = tempDir.resolve("bulk1.txt");
            Path f2 = tempDir.resolve("bulk2.txt");
            Files.write(f1, "data1".getBytes());
            Files.write(f2, "data2".getBytes());

            FileAttachment a1 = buildAttachment(UUID.randomUUID(), f1.toString());
            FileAttachment a2 = buildAttachment(UUID.randomUUID(), f2.toString());

            when(fileAttachmentRepository.findByMessageId(MESSAGE_ID)).thenReturn(List.of(a1, a2));

            fileAttachmentService.deleteAllAttachmentsForMessage(MESSAGE_ID);

            verify(fileAttachmentRepository).deleteAll(List.of(a1, a2));
            assertThat(Files.exists(f1)).isFalse();
            assertThat(Files.exists(f2)).isFalse();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    @Nested
    class HelperTests {

        @Test
        void sanitizeFileName_specialChars() {
            assertThat(FileAttachmentService.sanitizeFileName("my file (1).txt"))
                    .isEqualTo("my_file_1_.txt");
            assertThat(FileAttachmentService.sanitizeFileName("hello-world_v2.pdf"))
                    .isEqualTo("hello-world_v2.pdf");
            assertThat(FileAttachmentService.sanitizeFileName("a@b#c.doc"))
                    .isEqualTo("a_b_c.doc");
        }

        @Test
        void detectMimeType_knownExtensions() {
            assertThat(FileAttachmentService.detectMimeType("photo.png")).isEqualTo("image/png");
            assertThat(FileAttachmentService.detectMimeType("doc.pdf")).isEqualTo("application/pdf");
            assertThat(FileAttachmentService.detectMimeType("image.jpeg")).isEqualTo("image/jpeg");
            assertThat(FileAttachmentService.detectMimeType("data.json")).isEqualTo("application/json");
            assertThat(FileAttachmentService.detectMimeType("page.html")).isEqualTo("text/html");
        }

        @Test
        void detectMimeType_unknownExtension() {
            assertThat(FileAttachmentService.detectMimeType("file.xyz"))
                    .isEqualTo("application/octet-stream");
            assertThat(FileAttachmentService.detectMimeType("noextension"))
                    .isEqualTo("application/octet-stream");
        }
    }

    // ── Test Data Builders ───────────────────────────────────────────────

    private FileAttachment buildAttachment(UUID id, String storagePath) {
        FileAttachment attachment = FileAttachment.builder()
                .messageId(MESSAGE_ID)
                .uploadedBy(USER_ID)
                .teamId(CHANNEL_ID)
                .fileName("test.txt")
                .fileSizeBytes(100L)
                .contentType("text/plain")
                .storagePath(storagePath)
                .status(FileUploadStatus.COMPLETE)
                .build();
        attachment.setId(id);
        attachment.setCreatedAt(NOW);
        attachment.setUpdatedAt(NOW);
        return attachment;
    }
}
