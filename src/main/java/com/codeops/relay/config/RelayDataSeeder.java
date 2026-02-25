package com.codeops.relay.config;

import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.relay.dto.request.*;
import com.codeops.relay.dto.response.*;
import com.codeops.relay.entity.enums.ChannelType;
import com.codeops.relay.entity.enums.PlatformEventType;
import com.codeops.relay.entity.enums.PresenceStatus;
import com.codeops.relay.repository.ChannelRepository;
import com.codeops.relay.service.*;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Seeds development data for the Relay module.
 *
 * <p>Creates channels, messages, direct conversations, reactions,
 * presence records, and platform events using seeded user and team
 * data from the core {@code DataSeeder}.</p>
 *
 * <p>Only active under the {@code dev} profile. Skips seeding if
 * relay data already exists.</p>
 */
@Profile("dev")
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class RelayDataSeeder implements CommandLineRunner {

    private final ChannelService channelService;
    private final MessageService messageService;
    private final DirectMessageService directMessageService;
    private final ReactionService reactionService;
    private final PresenceService presenceService;
    private final PlatformEventService platformEventService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ChannelRepository channelRepository;

    private User adam, sarah, mike;
    private Team team;
    private UUID teamId;

    @Override
    @Transactional
    public void run(String... args) {
        loadReferences();
        if (team == null || adam == null) {
            log.warn("Relay seeder skipped — core data not yet available");
            return;
        }

        if (channelRepository.countByTeamId(teamId) > 0) {
            log.info("Relay data already seeded — skipping");
            return;
        }

        seedChannels();
        seedPresence();
        seedPlatformEvents();
        log.info("Relay development data seeded successfully");
    }

    private void loadReferences() {
        adam = userRepository.findByEmail("adam@allard.com").orElse(null);
        sarah = userRepository.findByEmail("sarah@codeops.dev").orElse(null);
        mike = userRepository.findByEmail("mike@codeops.dev").orElse(null);
        team = teamRepository.findAll().stream().findFirst().orElse(null);
        if (team != null) {
            teamId = team.getId();
        }
    }

    private void seedChannels() {
        // General channel
        ChannelResponse general = channelService.createChannel(
                new CreateChannelRequest("general", "General discussion for the team",
                        ChannelType.PUBLIC, "Welcome to CodeOps!"),
                teamId, adam.getId());

        // Dev updates channel
        ChannelResponse devUpdates = channelService.createChannel(
                new CreateChannelRequest("dev-updates", "Development updates and announcements",
                        ChannelType.PUBLIC, "Latest development news"),
                teamId, adam.getId());

        // Random channel
        ChannelResponse random = channelService.createChannel(
                new CreateChannelRequest("random", "Off-topic conversations",
                        ChannelType.PUBLIC, null),
                teamId, adam.getId());

        // Platform alerts channel (private)
        ChannelResponse alerts = channelService.createChannel(
                new CreateChannelRequest("platform-alerts", "Automated platform alerts and notifications",
                        ChannelType.PRIVATE, "Critical platform alerts"),
                teamId, adam.getId());

        // Join sarah and mike to public channels
        channelService.joinChannel(general.id(), teamId, sarah.getId());
        channelService.joinChannel(general.id(), teamId, mike.getId());
        channelService.joinChannel(devUpdates.id(), teamId, sarah.getId());
        channelService.joinChannel(random.id(), teamId, sarah.getId());
        channelService.joinChannel(random.id(), teamId, mike.getId());

        // Seed messages in general
        MessageResponse msg1 = messageService.sendMessage(general.id(),
                new SendMessageRequest("Welcome to the CodeOps Relay general channel!", null, null, null),
                teamId, adam.getId());
        MessageResponse msg2 = messageService.sendMessage(general.id(),
                new SendMessageRequest("Thanks Adam! Excited to try the new messaging system.", null, null, null),
                teamId, sarah.getId());
        messageService.sendMessage(general.id(),
                new SendMessageRequest("Looks great! The real-time features are impressive.", null, null, null),
                teamId, mike.getId());

        // Thread reply
        messageService.sendMessage(general.id(),
                new SendMessageRequest("Glad you like it! More features coming soon.", msg1.id(), null, null),
                teamId, adam.getId());

        // Messages in dev-updates
        messageService.sendMessage(devUpdates.id(),
                new SendMessageRequest("Deployed v2.1.0 — includes WebSocket support for real-time messaging.",
                        null, null, null),
                teamId, adam.getId());
        messageService.sendMessage(devUpdates.id(),
                new SendMessageRequest("Unit test coverage now at 95% across the Relay module.",
                        null, null, null),
                teamId, sarah.getId());

        // Reactions
        reactionService.toggleReaction(msg1.id(),
                new AddReactionRequest("\uD83D\uDC4D"), sarah.getId());
        reactionService.toggleReaction(msg1.id(),
                new AddReactionRequest("\uD83D\uDE80"), mike.getId());
        reactionService.toggleReaction(msg2.id(),
                new AddReactionRequest("❤️"), adam.getId());

        // DM conversation between adam and sarah
        DirectConversationResponse dmConvo = directMessageService.getOrCreateConversation(
                new CreateDirectConversationRequest(List.of(adam.getId(), sarah.getId()), null),
                teamId, adam.getId());

        directMessageService.sendDirectMessage(dmConvo.id(),
                new SendDirectMessageRequest("Hey Sarah, can you review the Relay PR?"),
                adam.getId());
        directMessageService.sendDirectMessage(dmConvo.id(),
                new SendDirectMessageRequest("Sure! I'll take a look this afternoon."),
                sarah.getId());

        log.info("Seeded 4 channels, messages, reactions, and 1 DM conversation");
    }

    private void seedPresence() {
        presenceService.updatePresence(
                new UpdatePresenceRequest(PresenceStatus.ONLINE, null),
                teamId, adam.getId());
        presenceService.updatePresence(
                new UpdatePresenceRequest(PresenceStatus.ONLINE, "Reviewing PRs"),
                teamId, sarah.getId());
        presenceService.updatePresence(
                new UpdatePresenceRequest(PresenceStatus.AWAY, "In a meeting"),
                teamId, mike.getId());
        log.info("Seeded presence for 3 users");
    }

    private void seedPlatformEvents() {
        platformEventService.publishEvent(
                PlatformEventType.DEPLOYMENT_COMPLETED, teamId,
                null, "CodeOps-Server",
                "Deployment completed: v2.1.0",
                "Successfully deployed CodeOps-Server v2.1.0 to production",
                adam.getId(), null);

        platformEventService.publishEvent(
                PlatformEventType.BUILD_COMPLETED, teamId,
                null, "CodeOps-Client",
                "Build completed: CodeOps-Client #142",
                "Flutter desktop build completed successfully",
                sarah.getId(), null);

        platformEventService.publishEvent(
                PlatformEventType.AUDIT_COMPLETED, teamId,
                null, "CodeOps-Server",
                "Security audit completed",
                "Automated security audit found 0 critical issues",
                adam.getId(), null);

        log.info("Seeded 3 platform events");
    }
}
