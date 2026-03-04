package com.codeops.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E Scenario 5: Relay Platform Events.
 *
 * <p>Tests Relay messaging: channel creation, messages, threads, reactions,
 * search, and direct messages. Platform events are read-only (auto-generated
 * by other modules), so we verify the events endpoint is accessible.
 */
@SuppressWarnings("unchecked")
class RelayPlatformEventsE2ETest extends E2ETestBase {

    private TestContext ctx;

    @BeforeEach
    void setUp() {
        ctx = setupFull("RelayEvents");
    }

    @Test
    void channelAndEvents() {
        // ── Step 1: Create project channel ──
        var channelBody = Map.of(
                "name", "dev-updates",
                "description", "Development updates channel",
                "channelType", "PROJECT",
                "topic", "Latest development activity"
        );
        ResponseEntity<Map> channelResp = post(
                "/api/v1/relay/channels?teamId=" + ctx.teamId(), ctx.token(), channelBody);
        assertThat(channelResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID channelId = extractId(channelResp.getBody());
        assertThat(channelResp.getBody().get("name")).isEqualTo("dev-updates");
        assertThat(channelResp.getBody().get("channelType")).isEqualTo("PROJECT");

        // ── Step 2: Post message to channel ──
        var msgBody = Map.of(
                "content", "Initial commit pushed to main branch"
        );
        ResponseEntity<Map> msgResp = post(
                "/api/v1/relay/channels/" + channelId + "/messages?teamId=" + ctx.teamId(),
                ctx.token(), msgBody);
        assertThat(msgResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID messageId = extractId(msgResp.getBody());
        assertThat(msgResp.getBody().get("content")).isEqualTo("Initial commit pushed to main branch");

        // ── Step 3: Create thread reply ──
        var threadBody = Map.of(
                "content", "Looks good! All tests passing.",
                "parentId", messageId.toString()
        );
        ResponseEntity<Map> threadResp = post(
                "/api/v1/relay/channels/" + channelId + "/messages?teamId=" + ctx.teamId(),
                ctx.token(), threadBody);
        assertThat(threadResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(threadResp.getBody().get("content")).isEqualTo("Looks good! All tests passing.");

        // ── Step 4: Add reaction to original message ──
        var reactionBody = Map.of("emoji", "\uD83D\uDE80");
        ResponseEntity<Map> reactionResp = post(
                "/api/v1/relay/reactions/messages/" + messageId + "/toggle",
                ctx.token(), reactionBody);
        assertThat(reactionResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify reaction exists
        ResponseEntity<List> reactionsResp = getList(
                "/api/v1/relay/reactions/messages/" + messageId, ctx.token());
        assertThat(reactionsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reactionsResp.getBody()).isNotEmpty();

        // ── Step 5: Search for message by content ──
        ResponseEntity<Map> searchResp = get(
                "/api/v1/relay/channels/" + channelId
                        + "/messages/search?query=commit&teamId=" + ctx.teamId(),
                ctx.token());
        assertThat(searchResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map searchPage = searchResp.getBody();
        List<Map> searchContent = (List<Map>) searchPage.get("content");
        assertThat(searchContent).isNotEmpty();
        boolean found = searchContent.stream()
                .anyMatch(m -> ((String) m.get("content")).contains("commit"));
        assertThat(found).isTrue();

        // ── Step 6: Create DM conversation with a second team member ──
        AuthResult user2 = registerAndLogin("Relay DM Partner");
        inviteAndAccept(ctx.token(), ctx.teamId(), user2, "MEMBER");
        // Re-login user2 so JWT includes team membership
        AuthResult user2Refreshed = login(user2.email(), user2.password());

        var dmBody = Map.of(
                "participantIds", List.of(user2Refreshed.userId().toString())
        );
        ResponseEntity<Map> dmResp = post(
                "/api/v1/relay/dm/conversations?teamId=" + ctx.teamId(), ctx.token(), dmBody);
        assertThat(dmResp.getStatusCode().is2xxSuccessful()).isTrue();

        UUID conversationId = extractId(dmResp.getBody());

        // Send a DM
        var dmMsgBody = Map.of("content", "Hey, can you review the PR?");
        ResponseEntity<Map> dmMsgResp = post(
                "/api/v1/relay/dm/conversations/" + conversationId + "/messages",
                ctx.token(), dmMsgBody);
        assertThat(dmMsgResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(dmMsgResp.getBody().get("content")).isEqualTo("Hey, can you review the PR?");

        // ── Step 7: Verify platform events endpoint is accessible ──
        ResponseEntity<Map> eventsResp = get(
                "/api/v1/relay/events?teamId=" + ctx.teamId() + "&page=0&size=20", ctx.token());
        assertThat(eventsResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ── Step 8: Post another message for thread verification ──
        ResponseEntity<List> threadReplies = getList(
                "/api/v1/relay/channels/" + channelId + "/messages/" + messageId + "/thread",
                ctx.token());
        assertThat(threadReplies.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(threadReplies.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }
}
