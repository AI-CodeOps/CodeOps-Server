package com.codeops.relay.service;

import com.codeops.config.AppConstants;
import com.codeops.entity.Team;
import com.codeops.entity.TeamMember;
import com.codeops.entity.User;
import com.codeops.entity.enums.TeamRole;
import com.codeops.exception.AuthorizationException;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.relay.dto.mapper.ChannelMapper;
import com.codeops.relay.dto.mapper.ChannelMemberMapper;
import com.codeops.relay.dto.request.CreateChannelRequest;
import com.codeops.relay.dto.request.InviteMemberRequest;
import com.codeops.relay.dto.request.PinMessageRequest;
import com.codeops.relay.dto.request.UpdateChannelRequest;
import com.codeops.relay.dto.request.UpdateChannelTopicRequest;
import com.codeops.relay.dto.request.UpdateMemberRoleRequest;
import com.codeops.relay.dto.response.ChannelMemberResponse;
import com.codeops.relay.dto.response.ChannelResponse;
import com.codeops.relay.dto.response.ChannelSummaryResponse;
import com.codeops.relay.dto.response.PinnedMessageResponse;
import com.codeops.relay.entity.Channel;
import com.codeops.relay.entity.ChannelMember;
import com.codeops.relay.entity.Message;
import com.codeops.relay.entity.PinnedMessage;
import com.codeops.relay.entity.ReadReceipt;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.MemberRole;
import com.codeops.relay.entity.enums.MessageType;
import com.codeops.relay.repository.ChannelMemberRepository;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.repository.MessageRepository;
import com.codeops.relay.repository.PinnedMessageRepository;
import com.codeops.relay.repository.ReadReceiptRepository;
import com.codeops.repository.TeamMemberRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChannelService}.
 *
 * <p>Uses Mockito to isolate the service from its dependencies. Covers channel CRUD,
 * topic management, member lifecycle (join/leave/invite/remove/role), pinned messages,
 * auto-channel creation, and slug generation.</p>
 */
@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelMemberRepository channelMemberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ReadReceiptRepository readReceiptRepository;
    @Mock private PinnedMessageRepository pinnedMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private ChannelMapper channelMapper;
    @Mock private ChannelMemberMapper channelMemberMapper;

    @InjectMocks private ChannelService channelService;

    private static final UUID TEAM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CHANNEL_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private Channel publicChannel;
    private Channel privateChannel;
    private User testUser;

    @BeforeEach
    void setUp() {
        publicChannel = Channel.builder()
                .name("General")
                .slug("general")
                .channelType(ChannelType.PUBLIC)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        publicChannel.setId(CHANNEL_ID);
        publicChannel.setCreatedAt(NOW);
        publicChannel.setUpdatedAt(NOW);

        privateChannel = Channel.builder()
                .name("Secret")
                .slug("secret")
                .channelType(ChannelType.PRIVATE)
                .teamId(TEAM_ID)
                .createdBy(USER_ID)
                .build();
        privateChannel.setId(UUID.randomUUID());
        privateChannel.setCreatedAt(NOW);
        privateChannel.setUpdatedAt(NOW);

        testUser = User.builder()
                .email("john@test.com")
                .displayName("John Doe")
                .passwordHash("hash")
                .build();
        testUser.setId(USER_ID);
    }

    // ── Channel CRUD ──────────────────────────────────────────────────────

    @Nested
    class CreateChannelTests {

        @Test
        void createChannel_success() {
            var request = new CreateChannelRequest("General", "Main channel",
                    ChannelType.PUBLIC, null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(channelRepository.existsByTeamIdAndSlug(TEAM_ID, "general")).thenReturn(false);
            when(channelMapper.toEntity(request)).thenReturn(Channel.builder()
                    .name("General").channelType(ChannelType.PUBLIC).build());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(CHANNEL_ID);
                c.setCreatedAt(NOW);
                c.setUpdatedAt(NOW);
                return c;
            });
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChannelResponse result = channelService.createChannel(request, TEAM_ID, USER_ID);

            assertThat(result.name()).isEqualTo("General");
            assertThat(result.slug()).isEqualTo("general");
            assertThat(result.memberCount()).isEqualTo(1);
            assertThat(result.channelType()).isEqualTo(ChannelType.PUBLIC);

            ArgumentCaptor<ChannelMember> memberCaptor = ArgumentCaptor.forClass(ChannelMember.class);
            verify(channelMemberRepository).save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getRole()).isEqualTo(MemberRole.OWNER);
        }

        @Test
        void createChannel_duplicateSlug_appendsSuffix() {
            var request = new CreateChannelRequest("General", null, ChannelType.PUBLIC, null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(channelRepository.existsByTeamIdAndSlug(TEAM_ID, "general")).thenReturn(true);
            when(channelRepository.existsByTeamIdAndSlug(TEAM_ID, "general-1")).thenReturn(false);
            when(channelMapper.toEntity(request)).thenReturn(Channel.builder()
                    .name("General").channelType(ChannelType.PUBLIC).build());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(CHANNEL_ID);
                c.setCreatedAt(NOW);
                c.setUpdatedAt(NOW);
                return c;
            });
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ChannelResponse result = channelService.createChannel(request, TEAM_ID, USER_ID);

            assertThat(result.slug()).isEqualTo("general-1");
        }

        @Test
        void createChannel_blankName_throwsValidation() {
            var request = new CreateChannelRequest("   ", null, ChannelType.PUBLIC, null);
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> channelService.createChannel(request, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("blank");
        }
    }

    @Nested
    class GetChannelTests {

        @Test
        void getChannel_success() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(5L);

            ChannelResponse result = channelService.getChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            assertThat(result.id()).isEqualTo(CHANNEL_ID);
            assertThat(result.name()).isEqualTo("General");
            assertThat(result.memberCount()).isEqualTo(5);
        }

        @Test
        void getChannel_privateChannel_nonMember_throwsAuth() {
            when(channelRepository.findById(privateChannel.getId()))
                    .thenReturn(Optional.of(privateChannel));
            when(channelMemberRepository.existsByChannelIdAndUserId(privateChannel.getId(), USER_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> channelService.getChannel(
                    privateChannel.getId(), TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("private");
        }
    }

    @Nested
    class GetChannelsByTeamTests {

        @Test
        void getChannelsByTeam_filtersPrivateForNonMembers() {
            when(channelRepository.findByTeamIdAndIsArchivedFalse(TEAM_ID))
                    .thenReturn(List.of(publicChannel, privateChannel));
            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID));
            when(channelMemberRepository.countByChannelId(any(UUID.class))).thenReturn(3L);
            when(messageRepository.findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                    any(UUID.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            List<ChannelSummaryResponse> result = channelService.getChannelsByTeam(TEAM_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("General");
        }

        @Test
        void getChannelsByTeam_includesUnreadCounts() {
            ChannelMember membership = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID)
                    .lastReadAt(NOW.minusSeconds(3600)).build();

            when(channelRepository.findByTeamIdAndIsArchivedFalse(TEAM_ID))
                    .thenReturn(List.of(publicChannel));
            when(channelMemberRepository.findChannelIdsByUserId(USER_ID))
                    .thenReturn(List.of(CHANNEL_ID));
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(3L);
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(membership));
            when(messageRepository.countUnreadMessages(eq(CHANNEL_ID), any(Instant.class)))
                    .thenReturn(7L);
            when(messageRepository.findByChannelIdAndIsDeletedFalseOrderByCreatedAtDesc(
                    eq(CHANNEL_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            List<ChannelSummaryResponse> result = channelService.getChannelsByTeam(TEAM_ID, USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).unreadCount()).isEqualTo(7L);
        }
    }

    @Nested
    class UpdateChannelTests {

        @Test
        void updateChannel_asOwner_success() {
            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            var request = new UpdateChannelRequest(null, "New description", null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(channelRepository.save(any(Channel.class))).thenReturn(publicChannel);
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(5L);

            ChannelResponse result = channelService.updateChannel(CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(publicChannel.getDescription()).isEqualTo("New description");
        }

        @Test
        void updateChannel_asMember_throwsAuth() {
            ChannelMember memberRole = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            var request = new UpdateChannelRequest("New Name", null, null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(memberRole));

            assertThatThrownBy(() -> channelService.updateChannel(CHANNEL_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void updateChannel_nameChange_regeneratesSlug() {
            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            var request = new UpdateChannelRequest("New Name", null, null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(channelRepository.existsByTeamIdAndSlug(TEAM_ID, "new-name")).thenReturn(false);
            when(channelRepository.save(any(Channel.class))).thenReturn(publicChannel);
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(5L);

            channelService.updateChannel(CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(publicChannel.getName()).isEqualTo("New Name");
            assertThat(publicChannel.getSlug()).isEqualTo("new-name");
        }
    }

    @Nested
    class DeleteChannelTests {

        @Test
        void deleteChannel_asOwner_success() {
            Channel deletable = Channel.builder()
                    .name("Random").slug("random").channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID).createdBy(USER_ID).build();
            deletable.setId(CHANNEL_ID);

            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(deletable));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));

            channelService.deleteChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            verify(messageRepository).deleteByChannelId(CHANNEL_ID);
            verify(readReceiptRepository).deleteByChannelId(CHANNEL_ID);
            verify(channelRepository).delete(deletable);
        }

        @Test
        void deleteChannel_general_throwsValidation() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));

            assertThatThrownBy(() -> channelService.deleteChannel(CHANNEL_ID, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("#general");
        }
    }

    @Nested
    class ArchiveChannelTests {

        @Test
        void archiveChannel_success() {
            Channel archivable = Channel.builder()
                    .name("Archive Me").slug("archive-me").channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID).createdBy(USER_ID).build();
            archivable.setId(CHANNEL_ID);
            archivable.setCreatedAt(NOW);
            archivable.setUpdatedAt(NOW);

            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(archivable));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(3L);

            ChannelResponse result = channelService.archiveChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            assertThat(result.isArchived()).isTrue();
        }

        @Test
        void unarchiveChannel_success() {
            Channel archived = Channel.builder()
                    .name("Archived").slug("archived").channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID).createdBy(USER_ID).isArchived(true).build();
            archived.setId(CHANNEL_ID);
            archived.setCreatedAt(NOW);
            archived.setUpdatedAt(NOW);

            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(archived));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> inv.getArgument(0));
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(3L);

            ChannelResponse result = channelService.unarchiveChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            assertThat(result.isArchived()).isFalse();
        }
    }

    // ── Topic ─────────────────────────────────────────────────────────────

    @Nested
    class TopicTests {

        @Test
        void updateTopic_asMember_success() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            var request = new UpdateChannelTopicRequest("New Topic!");

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(channelRepository.save(any(Channel.class))).thenReturn(publicChannel);
            when(channelMemberRepository.countByChannelId(CHANNEL_ID)).thenReturn(5L);

            ChannelResponse result = channelService.updateTopic(CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(publicChannel.getTopic()).isEqualTo("New Topic!");
        }
    }

    // ── Member Management ─────────────────────────────────────────────────

    @Nested
    class JoinChannelTests {

        @Test
        void joinChannel_public_success() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(false);
            when(channelMemberRepository.save(any(ChannelMember.class))).thenAnswer(inv -> {
                ChannelMember m = inv.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
            });
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            ChannelMemberResponse result = channelService.joinChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            assertThat(result.userId()).isEqualTo(USER_ID);
            assertThat(result.userDisplayName()).isEqualTo("John Doe");
        }

        @Test
        void joinChannel_private_throwsAuth() {
            when(channelRepository.findById(privateChannel.getId()))
                    .thenReturn(Optional.of(privateChannel));

            assertThatThrownBy(() -> channelService.joinChannel(
                    privateChannel.getId(), TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("invite");
        }

        @Test
        void joinChannel_alreadyMember_throwsValidation() {
            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> channelService.joinChannel(CHANNEL_ID, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already a member");
        }
    }

    @Nested
    class LeaveChannelTests {

        @Test
        void leaveChannel_success() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            member.setId(UUID.randomUUID());

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.empty());

            channelService.leaveChannel(CHANNEL_ID, TEAM_ID, USER_ID);

            verify(channelMemberRepository).delete(member);
        }

        @Test
        void leaveChannel_lastOwner_throwsValidation() {
            ChannelMember owner = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            ChannelMember regularMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(OTHER_USER_ID).role(MemberRole.MEMBER).build();

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(owner));
            when(channelMemberRepository.findByChannelId(CHANNEL_ID))
                    .thenReturn(List.of(owner, regularMember));

            assertThatThrownBy(() -> channelService.leaveChannel(CHANNEL_ID, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("last owner");
        }
    }

    @Nested
    class InviteMemberTests {

        @Test
        void inviteMember_asAdmin_success() {
            ChannelMember admin = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.ADMIN).build();
            var request = new InviteMemberRequest(OTHER_USER_ID, null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(admin));
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                    .thenReturn(true);
            when(channelMemberRepository.existsByChannelIdAndUserId(CHANNEL_ID, OTHER_USER_ID))
                    .thenReturn(false);
            when(channelMemberRepository.save(any(ChannelMember.class))).thenAnswer(inv -> {
                ChannelMember m = inv.getArgument(0);
                m.setId(UUID.randomUUID());
                return m;
            });
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(testUser));

            ChannelMemberResponse result = channelService.inviteMember(
                    CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(result.userId()).isEqualTo(OTHER_USER_ID);
            assertThat(result.role()).isEqualTo(MemberRole.MEMBER);
        }

        @Test
        void inviteMember_asMember_throwsAuth() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            var request = new InviteMemberRequest(OTHER_USER_ID, null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            assertThatThrownBy(() -> channelService.inviteMember(
                    CHANNEL_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void inviteMember_notTeamMember_throwsValidation() {
            ChannelMember owner = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            var request = new InviteMemberRequest(OTHER_USER_ID, null);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(owner));
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, OTHER_USER_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> channelService.inviteMember(
                    CHANNEL_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("not a member of this team");
        }
    }

    @Nested
    class RemoveMemberTests {

        @Test
        void removeMember_asOwner_success() {
            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            ChannelMember targetMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(OTHER_USER_ID).role(MemberRole.MEMBER).build();

            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, OTHER_USER_ID))
                    .thenReturn(Optional.of(targetMember));
            when(readReceiptRepository.findByChannelIdAndUserId(CHANNEL_ID, OTHER_USER_ID))
                    .thenReturn(Optional.empty());

            channelService.removeMember(CHANNEL_ID, OTHER_USER_ID, TEAM_ID, USER_ID);

            verify(channelMemberRepository).delete(targetMember);
        }

        @Test
        void removeMember_self_throwsValidation() {
            assertThatThrownBy(() -> channelService.removeMember(
                    CHANNEL_ID, USER_ID, TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Cannot remove yourself");
        }
    }

    @Nested
    class UpdateMemberRoleTests {

        @Test
        void updateMemberRole_asOwner_success() {
            ChannelMember ownerMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER).build();
            ChannelMember targetMember = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(OTHER_USER_ID).role(MemberRole.MEMBER)
                    .joinedAt(NOW).build();
            targetMember.setId(UUID.randomUUID());

            TeamMember teamMember = TeamMember.builder().role(TeamRole.OWNER).build();

            var request = new UpdateMemberRoleRequest(MemberRole.ADMIN);

            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(ownerMember));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(teamMember));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, OTHER_USER_ID))
                    .thenReturn(Optional.of(targetMember));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(testUser));

            ChannelMemberResponse result = channelService.updateMemberRole(
                    CHANNEL_ID, OTHER_USER_ID, request, TEAM_ID, USER_ID);

            assertThat(result.role()).isEqualTo(MemberRole.ADMIN);
        }

        @Test
        void updateMemberRole_asMember_throwsAuth() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            TeamMember teamMember = TeamMember.builder().role(TeamRole.MEMBER).build();
            var request = new UpdateMemberRoleRequest(MemberRole.ADMIN);

            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(teamMemberRepository.findByTeamIdAndUserId(TEAM_ID, USER_ID))
                    .thenReturn(Optional.of(teamMember));

            assertThatThrownBy(() -> channelService.updateMemberRole(
                    CHANNEL_ID, OTHER_USER_ID, request, TEAM_ID, USER_ID))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    @Nested
    class GetMembersTests {

        @Test
        void getMembers_populatesDisplayNames() {
            ChannelMember member1 = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.OWNER)
                    .joinedAt(NOW).build();
            member1.setId(UUID.randomUUID());
            ChannelMember member2 = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(OTHER_USER_ID).role(MemberRole.MEMBER)
                    .joinedAt(NOW).build();
            member2.setId(UUID.randomUUID());

            User otherUser = User.builder()
                    .email("jane@test.com").displayName("Jane Smith").passwordHash("hash").build();
            otherUser.setId(OTHER_USER_ID);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelId(CHANNEL_ID))
                    .thenReturn(List.of(member1, member2));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(otherUser));

            List<ChannelMemberResponse> result = channelService.getMembers(CHANNEL_ID, TEAM_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).userDisplayName()).isEqualTo("John Doe");
            assertThat(result.get(1).userDisplayName()).isEqualTo("Jane Smith");
        }
    }

    // ── Pinned Messages ───────────────────────────────────────────────────

    @Nested
    class PinMessageTests {

        @Test
        void pinMessage_success() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            Message message = Message.builder()
                    .channelId(CHANNEL_ID).senderId(USER_ID).content("Pin me!")
                    .messageType(MessageType.TEXT).build();
            message.setId(UUID.randomUUID());
            message.setCreatedAt(NOW);
            message.setUpdatedAt(NOW);

            var request = new PinMessageRequest(message.getId());

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(messageRepository.findById(message.getId())).thenReturn(Optional.of(message));
            when(pinnedMessageRepository.existsByChannelIdAndMessageId(CHANNEL_ID, message.getId()))
                    .thenReturn(false);
            when(pinnedMessageRepository.countByChannelId(CHANNEL_ID)).thenReturn(0L);
            when(pinnedMessageRepository.save(any(PinnedMessage.class))).thenAnswer(inv -> {
                PinnedMessage p = inv.getArgument(0);
                p.setId(UUID.randomUUID());
                p.setCreatedAt(NOW);
                return p;
            });
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            PinnedMessageResponse result = channelService.pinMessage(
                    CHANNEL_ID, request, TEAM_ID, USER_ID);

            assertThat(result.messageId()).isEqualTo(message.getId());
            assertThat(result.message()).isNotNull();
            assertThat(result.message().content()).isEqualTo("Pin me!");
        }

        @Test
        void pinMessage_alreadyPinned_throwsValidation() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            UUID messageId = UUID.randomUUID();
            Message message = Message.builder()
                    .channelId(CHANNEL_ID).senderId(USER_ID).content("Test")
                    .messageType(MessageType.TEXT).build();
            message.setId(messageId);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(pinnedMessageRepository.existsByChannelIdAndMessageId(CHANNEL_ID, messageId))
                    .thenReturn(true);

            assertThatThrownBy(() -> channelService.pinMessage(
                    CHANNEL_ID, new PinMessageRequest(messageId), TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already pinned");
        }

        @Test
        void pinMessage_maxPins_throwsValidation() {
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            UUID messageId = UUID.randomUUID();
            Message message = Message.builder()
                    .channelId(CHANNEL_ID).senderId(USER_ID).content("Test")
                    .messageType(MessageType.TEXT).build();
            message.setId(messageId);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(pinnedMessageRepository.existsByChannelIdAndMessageId(CHANNEL_ID, messageId))
                    .thenReturn(false);
            when(pinnedMessageRepository.countByChannelId(CHANNEL_ID))
                    .thenReturn((long) AppConstants.RELAY_MAX_PINS_PER_CHANNEL);

            assertThatThrownBy(() -> channelService.pinMessage(
                    CHANNEL_ID, new PinMessageRequest(messageId), TEAM_ID, USER_ID))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("maximum");
        }
    }

    @Nested
    class UnpinAndGetPinsTests {

        @Test
        void unpinMessage_success() {
            UUID messageId = UUID.randomUUID();
            ChannelMember member = ChannelMember.builder()
                    .channelId(CHANNEL_ID).userId(USER_ID).role(MemberRole.MEMBER).build();
            PinnedMessage pin = PinnedMessage.builder()
                    .messageId(messageId).pinnedBy(USER_ID).channel(publicChannel).build();

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(channelMemberRepository.findByChannelIdAndUserId(CHANNEL_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            when(pinnedMessageRepository.findByChannelIdAndMessageId(CHANNEL_ID, messageId))
                    .thenReturn(Optional.of(pin));

            channelService.unpinMessage(CHANNEL_ID, messageId, TEAM_ID, USER_ID);

            verify(pinnedMessageRepository).delete(pin);
        }

        @Test
        void getPinnedMessages_success() {
            UUID messageId = UUID.randomUUID();
            PinnedMessage pin = PinnedMessage.builder()
                    .messageId(messageId).pinnedBy(USER_ID).channel(publicChannel).build();
            pin.setId(UUID.randomUUID());
            pin.setCreatedAt(NOW);

            Message message = Message.builder()
                    .channelId(CHANNEL_ID).senderId(USER_ID).content("Pinned!")
                    .messageType(MessageType.TEXT).build();
            message.setId(messageId);
            message.setCreatedAt(NOW);
            message.setUpdatedAt(NOW);

            when(channelRepository.findById(CHANNEL_ID)).thenReturn(Optional.of(publicChannel));
            when(pinnedMessageRepository.findByChannelIdOrderByCreatedAtDesc(CHANNEL_ID))
                    .thenReturn(List.of(pin));
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));

            List<PinnedMessageResponse> result = channelService.getPinnedMessages(CHANNEL_ID, TEAM_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).message().content()).isEqualTo("Pinned!");
            assertThat(result.get(0).message().senderDisplayName()).isEqualTo("John Doe");
        }
    }

    // ── Auto-Channel Creation ─────────────────────────────────────────────

    @Nested
    class AutoChannelTests {

        @Test
        void createProjectChannel_success() {
            UUID projectId = UUID.randomUUID();
            User teamUser = User.builder().email("u@t.com").displayName("U").passwordHash("h").build();
            teamUser.setId(USER_ID);
            TeamMember tm = TeamMember.builder().role(TeamRole.MEMBER)
                    .user(teamUser).joinedAt(NOW).build();

            when(channelRepository.existsByTeamIdAndSlug(eq(TEAM_ID), any(String.class)))
                    .thenReturn(false);
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });
            when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(tm));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Channel result = channelService.createProjectChannel(projectId, "API Gateway", TEAM_ID);

            assertThat(result).isNotNull();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.PROJECT);
            assertThat(result.getProjectId()).isEqualTo(projectId);
            verify(channelMemberRepository).save(any(ChannelMember.class));
        }

        @Test
        void createServiceChannel_success() {
            UUID serviceId = UUID.randomUUID();
            User adminUser = User.builder().email("a@t.com").displayName("Admin").passwordHash("h").build();
            adminUser.setId(USER_ID);
            TeamMember admin = TeamMember.builder().role(TeamRole.ADMIN)
                    .user(adminUser).joinedAt(NOW).build();
            User memberUser = User.builder().email("m@t.com").displayName("Member").passwordHash("h").build();
            memberUser.setId(OTHER_USER_ID);
            TeamMember member = TeamMember.builder().role(TeamRole.MEMBER)
                    .user(memberUser).joinedAt(NOW).build();

            when(channelRepository.existsByTeamIdAndSlug(eq(TEAM_ID), any(String.class)))
                    .thenReturn(false);
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });
            when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(admin, member));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Channel result = channelService.createServiceChannel(serviceId, "Auth Service", TEAM_ID);

            assertThat(result).isNotNull();
            assertThat(result.getChannelType()).isEqualTo(ChannelType.SERVICE);
            assertThat(result.getServiceId()).isEqualTo(serviceId);

            ArgumentCaptor<ChannelMember> captor = ArgumentCaptor.forClass(ChannelMember.class);
            verify(channelMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        void ensureGeneralChannel_createsIfMissing() {
            User creator = User.builder().email("c@t.com").displayName("C").passwordHash("h").build();
            creator.setId(USER_ID);
            TeamMember tm = TeamMember.builder().role(TeamRole.OWNER)
                    .user(creator).joinedAt(NOW).build();

            when(channelRepository.findByTeamIdAndSlug(TEAM_ID,
                    AppConstants.RELAY_GENERAL_CHANNEL_SLUG)).thenReturn(Optional.empty());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });
            when(teamMemberRepository.findByTeamId(TEAM_ID)).thenReturn(List.of(tm));
            when(channelMemberRepository.save(any(ChannelMember.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Channel result = channelService.ensureGeneralChannel(TEAM_ID, USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.getSlug()).isEqualTo(AppConstants.RELAY_GENERAL_CHANNEL_SLUG);
            assertThat(result.getChannelType()).isEqualTo(ChannelType.PUBLIC);
            verify(channelRepository).save(any(Channel.class));
        }

        @Test
        void ensureGeneralChannel_returnsExisting() {
            Channel existing = Channel.builder()
                    .name("#general").slug("general").channelType(ChannelType.PUBLIC)
                    .teamId(TEAM_ID).createdBy(USER_ID).build();
            existing.setId(UUID.randomUUID());

            when(channelRepository.findByTeamIdAndSlug(TEAM_ID,
                    AppConstants.RELAY_GENERAL_CHANNEL_SLUG)).thenReturn(Optional.of(existing));

            Channel result = channelService.ensureGeneralChannel(TEAM_ID, USER_ID);

            assertThat(result.getId()).isEqualTo(existing.getId());
            verify(channelRepository, never()).save(any(Channel.class));
        }
    }

    // ── Slug Generation ───────────────────────────────────────────────────

    @Nested
    class SlugifyTests {

        @Test
        void slugify_variousInputs() {
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(channelRepository.existsByTeamIdAndSlug(eq(TEAM_ID), any(String.class)))
                    .thenReturn(false);
            when(channelMapper.toEntity(any())).thenReturn(Channel.builder()
                    .name("test").channelType(ChannelType.PUBLIC).build());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                c.setCreatedAt(NOW);
                c.setUpdatedAt(NOW);
                return c;
            });
            when(channelMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new CreateChannelRequest("Hello World 123", null, ChannelType.PUBLIC, null);
            ChannelResponse result = channelService.createChannel(request, TEAM_ID, USER_ID);

            ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
            verify(channelRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("hello-world-123");
        }

        @Test
        void slugify_specialCharacters() {
            when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, USER_ID)).thenReturn(true);
            when(channelRepository.existsByTeamIdAndSlug(eq(TEAM_ID), any(String.class)))
                    .thenReturn(false);
            when(channelMapper.toEntity(any())).thenReturn(Channel.builder()
                    .name("test").channelType(ChannelType.PUBLIC).build());
            when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
                Channel c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                c.setCreatedAt(NOW);
                c.setUpdatedAt(NOW);
                return c;
            });
            when(channelMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var request = new CreateChannelRequest("  --API!! Gateway v2.0--  ", null,
                    ChannelType.PUBLIC, null);
            channelService.createChannel(request, TEAM_ID, USER_ID);

            ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);
            verify(channelRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("api-gateway-v20");
        }
    }
}
