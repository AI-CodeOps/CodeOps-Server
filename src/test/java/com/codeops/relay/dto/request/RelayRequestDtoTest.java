package com.codeops.relay.dto.request;

import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.entity.enums.PresenceStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests for all Relay module request DTOs.
 *
 * <p>Verifies Jakarta Validation constraints ({@code @NotBlank}, {@code @NotNull},
 * {@code @Size}, {@code @NotEmpty}) on every request record.</p>
 */
class RelayRequestDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ── CreateChannelRequest ──────────────────────────────────────────────

    @Nested
    class CreateChannelRequestTests {

        @Test
        void valid_noViolations() {
            var req = new CreateChannelRequest("General", "Main channel", ChannelType.PUBLIC, "Welcome");
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullName_violates() {
            var req = new CreateChannelRequest(null, null, ChannelType.PUBLIC, null);
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        void blankName_violates() {
            var req = new CreateChannelRequest("   ", null, ChannelType.PUBLIC, null);
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        void nameTooLong_violates() {
            var req = new CreateChannelRequest("x".repeat(101), null, ChannelType.PUBLIC, null);
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }

        @Test
        void nullChannelType_violates() {
            var req = new CreateChannelRequest("General", null, null, null);
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("channelType"));
        }

        @Test
        void topicTooLong_violates() {
            var req = new CreateChannelRequest("General", null, ChannelType.PUBLIC, "x".repeat(501));
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("topic"));
        }

        @Test
        void optionalFieldsNull_noViolations() {
            var req = new CreateChannelRequest("General", null, ChannelType.PRIVATE, null);
            Set<ConstraintViolation<CreateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    // ── UpdateChannelRequest ──────────────────────────────────────────────

    @Nested
    class UpdateChannelRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdateChannelRequest("Updated", "New desc", true);
            Set<ConstraintViolation<UpdateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void allFieldsNull_noViolations() {
            var req = new UpdateChannelRequest(null, null, null);
            Set<ConstraintViolation<UpdateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nameTooLong_violates() {
            var req = new UpdateChannelRequest("x".repeat(101), null, null);
            Set<ConstraintViolation<UpdateChannelRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
        }
    }

    // ── UpdateChannelTopicRequest ─────────────────────────────────────────

    @Nested
    class UpdateChannelTopicRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdateChannelTopicRequest("New topic");
            Set<ConstraintViolation<UpdateChannelTopicRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullTopic_noViolations() {
            var req = new UpdateChannelTopicRequest(null);
            Set<ConstraintViolation<UpdateChannelTopicRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void topicTooLong_violates() {
            var req = new UpdateChannelTopicRequest("x".repeat(501));
            Set<ConstraintViolation<UpdateChannelTopicRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("topic"));
        }
    }

    // ── SendMessageRequest ────────────────────────────────────────────────

    @Nested
    class SendMessageRequestTests {

        @Test
        void valid_noViolations() {
            var req = new SendMessageRequest("Hello!", null, null, null);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void validWithAllFields_noViolations() {
            var req = new SendMessageRequest("Hello @all!", UUID.randomUUID(),
                    List.of(UUID.randomUUID()), true);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullContent_violates() {
            var req = new SendMessageRequest(null, null, null, null);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        void blankContent_violates() {
            var req = new SendMessageRequest("  ", null, null, null);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        void contentTooLong_violates() {
            var req = new SendMessageRequest("x".repeat(10001), null, null, null);
            Set<ConstraintViolation<SendMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }
    }

    // ── UpdateMessageRequest ──────────────────────────────────────────────

    @Nested
    class UpdateMessageRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdateMessageRequest("Updated content");
            Set<ConstraintViolation<UpdateMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankContent_violates() {
            var req = new UpdateMessageRequest("");
            Set<ConstraintViolation<UpdateMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        void contentTooLong_violates() {
            var req = new UpdateMessageRequest("x".repeat(10001));
            Set<ConstraintViolation<UpdateMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }
    }

    // ── CreateDirectConversationRequest ───────────────────────────────────

    @Nested
    class CreateDirectConversationRequestTests {

        @Test
        void valid_noViolations() {
            var req = new CreateDirectConversationRequest(List.of(UUID.randomUUID()), null);
            Set<ConstraintViolation<CreateDirectConversationRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void validWithName_noViolations() {
            var req = new CreateDirectConversationRequest(
                    List.of(UUID.randomUUID(), UUID.randomUUID()), "Group Chat");
            Set<ConstraintViolation<CreateDirectConversationRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void emptyParticipants_violates() {
            var req = new CreateDirectConversationRequest(List.of(), null);
            Set<ConstraintViolation<CreateDirectConversationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("participantIds"));
        }

        @Test
        void nullParticipants_violates() {
            var req = new CreateDirectConversationRequest(null, null);
            Set<ConstraintViolation<CreateDirectConversationRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("participantIds"));
        }
    }

    // ── SendDirectMessageRequest ──────────────────────────────────────────

    @Nested
    class SendDirectMessageRequestTests {

        @Test
        void valid_noViolations() {
            var req = new SendDirectMessageRequest("Hello DM!");
            Set<ConstraintViolation<SendDirectMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankContent_violates() {
            var req = new SendDirectMessageRequest("   ");
            Set<ConstraintViolation<SendDirectMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }

        @Test
        void contentTooLong_violates() {
            var req = new SendDirectMessageRequest("x".repeat(10001));
            Set<ConstraintViolation<SendDirectMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }
    }

    // ── UpdateDirectMessageRequest ────────────────────────────────────────

    @Nested
    class UpdateDirectMessageRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdateDirectMessageRequest("Edited DM");
            Set<ConstraintViolation<UpdateDirectMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankContent_violates() {
            var req = new UpdateDirectMessageRequest("");
            Set<ConstraintViolation<UpdateDirectMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));
        }
    }

    // ── AddReactionRequest ────────────────────────────────────────────────

    @Nested
    class AddReactionRequestTests {

        @Test
        void valid_noViolations() {
            var req = new AddReactionRequest("\uD83D\uDE00");
            Set<ConstraintViolation<AddReactionRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void blankEmoji_violates() {
            var req = new AddReactionRequest("  ");
            Set<ConstraintViolation<AddReactionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("emoji"));
        }

        @Test
        void nullEmoji_violates() {
            var req = new AddReactionRequest(null);
            Set<ConstraintViolation<AddReactionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("emoji"));
        }

        @Test
        void emojiTooLong_violates() {
            var req = new AddReactionRequest("x".repeat(51));
            Set<ConstraintViolation<AddReactionRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("emoji"));
        }
    }

    // ── UploadFileRequest ─────────────────────────────────────────────────

    @Nested
    class UploadFileRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UploadFileRequest(UUID.randomUUID(), null);
            Set<ConstraintViolation<UploadFileRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void allFieldsNull_noViolations() {
            var req = new UploadFileRequest(null, null);
            Set<ConstraintViolation<UploadFileRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    // ── UpdatePresenceRequest ─────────────────────────────────────────────

    @Nested
    class UpdatePresenceRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdatePresenceRequest(PresenceStatus.ONLINE, "Working");
            Set<ConstraintViolation<UpdatePresenceRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullStatus_violates() {
            var req = new UpdatePresenceRequest(null, null);
            Set<ConstraintViolation<UpdatePresenceRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
        }

        @Test
        void statusMessageTooLong_violates() {
            var req = new UpdatePresenceRequest(PresenceStatus.ONLINE, "x".repeat(201));
            Set<ConstraintViolation<UpdatePresenceRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("statusMessage"));
        }

        @Test
        void nullStatusMessage_noViolations() {
            var req = new UpdatePresenceRequest(PresenceStatus.AWAY, null);
            Set<ConstraintViolation<UpdatePresenceRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    // ── InviteMemberRequest ───────────────────────────────────────────────

    @Nested
    class InviteMemberRequestTests {

        @Test
        void valid_noViolations() {
            var req = new InviteMemberRequest(UUID.randomUUID(), MemberRole.MEMBER);
            Set<ConstraintViolation<InviteMemberRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullUserId_violates() {
            var req = new InviteMemberRequest(null, MemberRole.MEMBER);
            Set<ConstraintViolation<InviteMemberRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("userId"));
        }

        @Test
        void nullRole_noViolations() {
            var req = new InviteMemberRequest(UUID.randomUUID(), null);
            Set<ConstraintViolation<InviteMemberRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }
    }

    // ── UpdateMemberRoleRequest ───────────────────────────────────────────

    @Nested
    class UpdateMemberRoleRequestTests {

        @Test
        void valid_noViolations() {
            var req = new UpdateMemberRoleRequest(MemberRole.ADMIN);
            Set<ConstraintViolation<UpdateMemberRoleRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullRole_violates() {
            var req = new UpdateMemberRoleRequest(null);
            Set<ConstraintViolation<UpdateMemberRoleRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("role"));
        }
    }

    // ── PinMessageRequest ─────────────────────────────────────────────────

    @Nested
    class PinMessageRequestTests {

        @Test
        void valid_noViolations() {
            var req = new PinMessageRequest(UUID.randomUUID());
            Set<ConstraintViolation<PinMessageRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullMessageId_violates() {
            var req = new PinMessageRequest(null);
            Set<ConstraintViolation<PinMessageRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("messageId"));
        }
    }

    // ── MarkReadRequest ───────────────────────────────────────────────────

    @Nested
    class MarkReadRequestTests {

        @Test
        void valid_noViolations() {
            var req = new MarkReadRequest(UUID.randomUUID());
            Set<ConstraintViolation<MarkReadRequest>> violations = validator.validate(req);
            assertThat(violations).isEmpty();
        }

        @Test
        void nullLastReadMessageId_violates() {
            var req = new MarkReadRequest(null);
            Set<ConstraintViolation<MarkReadRequest>> violations = validator.validate(req);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("lastReadMessageId"));
        }
    }
}
