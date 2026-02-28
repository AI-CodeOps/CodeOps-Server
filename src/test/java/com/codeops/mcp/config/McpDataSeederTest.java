package com.codeops.mcp.config;

import com.codeops.entity.Project;
import com.codeops.entity.Team;
import com.codeops.entity.User;
import com.codeops.mcp.entity.*;
import com.codeops.mcp.entity.enums.*;
import com.codeops.mcp.repository.*;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.TeamRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link McpDataSeeder}.
 *
 * <p>Verifies that the seeder creates developer profiles, sessions,
 * documents, and activity feed entries when core data is available,
 * and skips seeding when data already exists.</p>
 */
@ExtendWith(MockitoExtension.class)
class McpDataSeederTest {

    @Mock DeveloperProfileRepository developerProfileRepository;
    @Mock McpApiTokenRepository apiTokenRepository;
    @Mock McpSessionRepository sessionRepository;
    @Mock SessionResultRepository sessionResultRepository;
    @Mock SessionToolCallRepository toolCallRepository;
    @Mock ProjectDocumentRepository documentRepository;
    @Mock ProjectDocumentVersionRepository versionRepository;
    @Mock ActivityFeedEntryRepository activityFeedEntryRepository;
    @Mock UserRepository userRepository;
    @Mock TeamRepository teamRepository;
    @Mock ProjectRepository projectRepository;

    @InjectMocks McpDataSeeder seeder;

    private User adam;
    private Team team;
    private Project project;

    @BeforeEach
    void setUp() {
        adam = new User();
        adam.setId(UUID.randomUUID());
        adam.setEmail("adam@allard.com");
        adam.setDisplayName("Adam Allard");

        team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Test Team");

        project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Test Project");
        project.setTeam(team);
    }

    // ── seed_createsProfiles ─────────────────────────────────────────────

    @Test
    void seed_createsProfiles() throws Exception {
        when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(projectRepository.findByTeamId(team.getId())).thenReturn(List.of(project));
        when(developerProfileRepository.count()).thenReturn(0L);
        when(developerProfileRepository.save(any())).thenAnswer(inv -> {
            DeveloperProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(apiTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            McpSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(toolCallRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            ProjectDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityFeedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<DeveloperProfile> profileCaptor = ArgumentCaptor.forClass(DeveloperProfile.class);
        verify(developerProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDisplayName()).isEqualTo("Adam Allard");
        assertThat(profileCaptor.getValue().getDefaultEnvironment()).isEqualTo(Environment.LOCAL);
    }

    // ── seed_createsSessions ─────────────────────────────────────────────

    @Test
    void seed_createsSessions() throws Exception {
        when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(projectRepository.findByTeamId(team.getId())).thenReturn(List.of(project));
        when(developerProfileRepository.count()).thenReturn(0L);
        when(developerProfileRepository.save(any())).thenAnswer(inv -> {
            DeveloperProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(apiTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            McpSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(toolCallRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            ProjectDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityFeedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<McpSession> sessionCaptor = ArgumentCaptor.forClass(McpSession.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(sessionCaptor.getValue().getTransport()).isEqualTo(McpTransport.HTTP);
    }

    // ── seed_createsDocuments ────────────────────────────────────────────

    @Test
    void seed_createsDocuments() throws Exception {
        when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(projectRepository.findByTeamId(team.getId())).thenReturn(List.of(project));
        when(developerProfileRepository.count()).thenReturn(0L);
        when(developerProfileRepository.save(any())).thenAnswer(inv -> {
            DeveloperProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(apiTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            McpSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(toolCallRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            ProjectDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityFeedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<ProjectDocument> docCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
        verify(documentRepository, times(2)).save(docCaptor.capture());
        List<ProjectDocument> docs = docCaptor.getAllValues();
        assertThat(docs).extracting(ProjectDocument::getDocumentType)
                .containsExactlyInAnyOrder(DocumentType.CLAUDE_MD, DocumentType.ARCHITECTURE_MD);
    }

    // ── seed_createsActivityFeed ─────────────────────────────────────────

    @Test
    void seed_createsActivityFeed() throws Exception {
        when(userRepository.findByEmail("adam@allard.com")).thenReturn(Optional.of(adam));
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(projectRepository.findByTeamId(team.getId())).thenReturn(List.of(project));
        when(developerProfileRepository.count()).thenReturn(0L);
        when(developerProfileRepository.save(any())).thenAnswer(inv -> {
            DeveloperProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(apiTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.save(any())).thenAnswer(inv -> {
            McpSession s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(toolCallRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sessionResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any())).thenAnswer(inv -> {
            ProjectDocument d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityFeedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seeder.run();

        ArgumentCaptor<ActivityFeedEntry> feedCaptor = ArgumentCaptor.forClass(ActivityFeedEntry.class);
        verify(activityFeedEntryRepository, times(2)).save(feedCaptor.capture());
        List<ActivityFeedEntry> entries = feedCaptor.getAllValues();
        assertThat(entries).extracting(ActivityFeedEntry::getActivityType)
                .containsExactlyInAnyOrder(ActivityType.SESSION_COMPLETED, ActivityType.DOCUMENT_UPDATED);
    }
}
