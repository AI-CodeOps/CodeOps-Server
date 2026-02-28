package com.codeops.mcp.service;

import com.codeops.entity.Project;
import com.codeops.entity.User;
import com.codeops.exception.NotFoundException;
import com.codeops.exception.ValidationException;
import com.codeops.mcp.dto.mapper.ProjectDocumentMapper;
import com.codeops.mcp.dto.mapper.ProjectDocumentVersionMapper;
import com.codeops.mcp.dto.request.CompleteSessionRequest.DocumentUpdate;
import com.codeops.mcp.dto.request.CreateProjectDocumentRequest;
import com.codeops.mcp.dto.request.UpdateProjectDocumentRequest;
import com.codeops.mcp.dto.response.ProjectDocumentDetailResponse;
import com.codeops.mcp.dto.response.ProjectDocumentResponse;
import com.codeops.mcp.dto.response.ProjectDocumentVersionResponse;
import com.codeops.mcp.entity.McpSession;
import com.codeops.mcp.entity.ProjectDocument;
import com.codeops.mcp.entity.ProjectDocumentVersion;
import com.codeops.mcp.entity.enums.AuthorType;
import com.codeops.mcp.entity.enums.DocumentType;
import com.codeops.mcp.entity.enums.Environment;
import com.codeops.mcp.entity.enums.McpTransport;
import com.codeops.mcp.entity.enums.SessionStatus;
import com.codeops.mcp.repository.McpSessionRepository;
import com.codeops.mcp.repository.ProjectDocumentRepository;
import com.codeops.mcp.repository.ProjectDocumentVersionRepository;
import com.codeops.repository.ProjectRepository;
import com.codeops.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentManagementService}.
 *
 * <p>Verifies document CRUD, versioning, session writeback, staleness
 * detection, and context map generation.</p>
 */
@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    @Mock private ProjectDocumentRepository documentRepository;
    @Mock private ProjectDocumentVersionRepository versionRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private McpSessionRepository sessionRepository;
    @Mock private ProjectDocumentMapper documentMapper;
    @Mock private ProjectDocumentVersionMapper versionMapper;

    @InjectMocks
    private DocumentManagementService service;

    // ── Test Data Builders ──

    private User createUser() {
        User user = User.builder()
                .displayName("Test Developer")
                .email("test@test.com")
                .passwordHash("hash")
                .build();
        user.setId(USER_ID);
        return user;
    }

    private Project createProject() {
        Project project = Project.builder()
                .name("CodeOps Server")
                .build();
        project.setId(PROJECT_ID);
        return project;
    }

    private ProjectDocument createDocument(DocumentType type) {
        ProjectDocument doc = ProjectDocument.builder()
                .documentType(type)
                .currentContent("# Content")
                .lastAuthorType(AuthorType.HUMAN)
                .project(createProject())
                .lastUpdatedBy(createUser())
                .build();
        doc.setId(DOCUMENT_ID);
        return doc;
    }

    private ProjectDocumentVersion createVersion(ProjectDocument document, int versionNumber) {
        ProjectDocumentVersion version = ProjectDocumentVersion.builder()
                .versionNumber(versionNumber)
                .content("Version " + versionNumber + " content")
                .authorType(AuthorType.HUMAN)
                .changeDescription("Change " + versionNumber)
                .document(document)
                .author(createUser())
                .build();
        version.setId(VERSION_ID);
        return version;
    }

    private ProjectDocumentResponse createDocResponse() {
        return new ProjectDocumentResponse(
                DOCUMENT_ID, DocumentType.CLAUDE_MD, null,
                AuthorType.HUMAN, null, false, null,
                PROJECT_ID, "Test Developer", NOW, NOW);
    }

    private ProjectDocumentDetailResponse createDetailResponse() {
        return new ProjectDocumentDetailResponse(
                DOCUMENT_ID, DocumentType.CLAUDE_MD, null,
                "# Content", AuthorType.HUMAN, null,
                false, null, PROJECT_ID, "Test Developer",
                List.of(), NOW, NOW);
    }

    private ProjectDocumentVersionResponse createVersionResponse(int versionNumber) {
        return new ProjectDocumentVersionResponse(
                VERSION_ID, versionNumber, "Version " + versionNumber + " content",
                AuthorType.HUMAN, null, "Change " + versionNumber,
                "Test Developer", null, NOW);
    }

    // ── createDocument ──

    @Nested
    @DisplayName("createDocument")
    class CreateDocumentTests {

        @Test
        @DisplayName("creates document and version 1")
        void createDocument_createsDocAndVersion1() {
            CreateProjectDocumentRequest request = new CreateProjectDocumentRequest(
                    DocumentType.CLAUDE_MD, null, "# Project Instructions",
                    "Initial creation", "abc123");

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(documentRepository.existsByProjectIdAndDocumentType(PROJECT_ID, DocumentType.CLAUDE_MD))
                    .thenReturn(false);
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> {
                        ProjectDocument doc = inv.getArgument(0);
                        doc.setId(DOCUMENT_ID);
                        return doc;
                    });
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> {
                        ProjectDocumentVersion v = inv.getArgument(0);
                        v.setId(VERSION_ID);
                        return v;
                    });
            when(versionMapper.toResponse(any(ProjectDocumentVersion.class)))
                    .thenReturn(createVersionResponse(1));
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            ProjectDocumentDetailResponse result = service.createDocument(
                    PROJECT_ID, request, USER_ID, AuthorType.HUMAN);

            assertThat(result).isNotNull();

            ArgumentCaptor<ProjectDocument> docCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.CLAUDE_MD);
            assertThat(docCaptor.getValue().getCurrentContent()).isEqualTo("# Project Instructions");

            ArgumentCaptor<ProjectDocumentVersion> versionCaptor =
                    ArgumentCaptor.forClass(ProjectDocumentVersion.class);
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(1);
            assertThat(versionCaptor.getValue().getContent()).isEqualTo("# Project Instructions");
            assertThat(versionCaptor.getValue().getCommitHash()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("custom type requires customName")
        void createDocument_customType_requiresCustomName() {
            CreateProjectDocumentRequest request = new CreateProjectDocumentRequest(
                    DocumentType.CUSTOM, null, "Content", "desc", null);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));

            assertThatThrownBy(() -> service.createDocument(PROJECT_ID, request, USER_ID, AuthorType.HUMAN))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("customName");
        }

        @Test
        @DisplayName("duplicate type throws validation exception")
        void createDocument_duplicateType_throwsConflict() {
            CreateProjectDocumentRequest request = new CreateProjectDocumentRequest(
                    DocumentType.CLAUDE_MD, null, "Content", "desc", null);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(documentRepository.existsByProjectIdAndDocumentType(PROJECT_ID, DocumentType.CLAUDE_MD))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createDocument(PROJECT_ID, request, USER_ID, AuthorType.HUMAN))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("already exists");
        }
    }

    // ── updateDocument ──

    @Nested
    @DisplayName("updateDocument")
    class UpdateDocumentTests {

        @Test
        @DisplayName("creates new version with incremented number")
        void updateDocument_createsNewVersion() {
            UpdateProjectDocumentRequest request = new UpdateProjectDocumentRequest(
                    "Updated content", "Fixed typo", "def456", AuthorType.HUMAN);

            ProjectDocument document = createDocument(DocumentType.CLAUDE_MD);
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(versionRepository.findMaxVersionNumber(DOCUMENT_ID)).thenReturn(2);
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> {
                        ProjectDocumentVersion v = inv.getArgument(0);
                        v.setId(UUID.randomUUID());
                        return v;
                    });
            when(documentRepository.save(any(ProjectDocument.class))).thenReturn(document);
            when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(DOCUMENT_ID))
                    .thenReturn(List.of());
            when(versionMapper.toResponseList(anyList())).thenReturn(List.of());
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            service.updateDocument(DOCUMENT_ID, request, USER_ID, null);

            ArgumentCaptor<ProjectDocumentVersion> captor =
                    ArgumentCaptor.forClass(ProjectDocumentVersion.class);
            verify(versionRepository).save(captor.capture());
            assertThat(captor.getValue().getVersionNumber()).isEqualTo(3);
            assertThat(captor.getValue().getContent()).isEqualTo("Updated content");
        }

        @Test
        @DisplayName("updates parent document currentContent")
        void updateDocument_updatesCurrentContent() {
            UpdateProjectDocumentRequest request = new UpdateProjectDocumentRequest(
                    "New content here", "Major rewrite", null, AuthorType.HUMAN);

            ProjectDocument document = createDocument(DocumentType.ARCHITECTURE_MD);
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(versionRepository.findMaxVersionNumber(DOCUMENT_ID)).thenReturn(1);
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(DOCUMENT_ID))
                    .thenReturn(List.of());
            when(versionMapper.toResponseList(anyList())).thenReturn(List.of());
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            service.updateDocument(DOCUMENT_ID, request, USER_ID, null);

            ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().getCurrentContent()).isEqualTo("New content here");
            assertThat(captor.getValue().isFlagged()).isFalse();
        }

        @Test
        @DisplayName("sets author type and session ID")
        void updateDocument_setsAuthorAndSession() {
            UpdateProjectDocumentRequest request = new UpdateProjectDocumentRequest(
                    "AI-generated content", "AI update", null, AuthorType.AI);

            ProjectDocument document = createDocument(DocumentType.AUDIT_MD);
            McpSession session = McpSession.builder()
                    .status(SessionStatus.ACTIVE)
                    .environment(Environment.DEVELOPMENT)
                    .transport(McpTransport.SSE)
                    .build();
            session.setId(SESSION_ID);

            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(versionRepository.findMaxVersionNumber(DOCUMENT_ID)).thenReturn(1);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(DOCUMENT_ID))
                    .thenReturn(List.of());
            when(versionMapper.toResponseList(anyList())).thenReturn(List.of());
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            service.updateDocument(DOCUMENT_ID, request, USER_ID, SESSION_ID);

            ArgumentCaptor<ProjectDocument> docCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getLastAuthorType()).isEqualTo(AuthorType.AI);
            assertThat(docCaptor.getValue().getLastSessionId()).isEqualTo(SESSION_ID);

            ArgumentCaptor<ProjectDocumentVersion> versionCaptor =
                    ArgumentCaptor.forClass(ProjectDocumentVersion.class);
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getAuthorType()).isEqualTo(AuthorType.AI);
            assertThat(versionCaptor.getValue().getSession()).isEqualTo(session);
        }
    }

    // ── getProjectDocuments ──

    @Nested
    @DisplayName("getProjectDocuments")
    class GetProjectDocumentsTests {

        @Test
        @DisplayName("returns all documents for project")
        void getProjectDocuments_returnsAll() {
            ProjectDocument doc1 = createDocument(DocumentType.CLAUDE_MD);
            ProjectDocument doc2 = createDocument(DocumentType.AUDIT_MD);
            doc2.setId(UUID.randomUUID());

            when(documentRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(doc1, doc2));
            when(documentMapper.toResponseList(anyList()))
                    .thenReturn(List.of(createDocResponse(), createDocResponse()));

            List<ProjectDocumentResponse> result = service.getProjectDocuments(PROJECT_ID);

            assertThat(result).hasSize(2);
            verify(documentRepository).findByProjectId(PROJECT_ID);
        }
    }

    // ── getDocument ──

    @Nested
    @DisplayName("getDocument")
    class GetDocumentTests {

        @Test
        @DisplayName("returns document by type")
        void getDocument_byType() {
            ProjectDocument document = createDocument(DocumentType.CLAUDE_MD);
            when(documentRepository.findByProjectIdAndDocumentType(PROJECT_ID, DocumentType.CLAUDE_MD))
                    .thenReturn(Optional.of(document));
            when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(DOCUMENT_ID))
                    .thenReturn(List.of());
            when(versionMapper.toResponseList(anyList())).thenReturn(List.of());
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            ProjectDocumentDetailResponse result = service.getDocument(PROJECT_ID, DocumentType.CLAUDE_MD);

            assertThat(result).isNotNull();
            verify(documentRepository).findByProjectIdAndDocumentType(PROJECT_ID, DocumentType.CLAUDE_MD);
        }

        @Test
        @DisplayName("throws when document type not found")
        void getDocument_notFound_throws() {
            when(documentRepository.findByProjectIdAndDocumentType(PROJECT_ID, DocumentType.OPENAPI_YAML))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDocument(PROJECT_ID, DocumentType.OPENAPI_YAML))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("ProjectDocument");
        }
    }

    // ── getCustomDocument ──

    @Nested
    @DisplayName("getCustomDocument")
    class GetCustomDocumentTests {

        @Test
        @DisplayName("returns custom document by name")
        void getCustomDocument_byName() {
            ProjectDocument document = createDocument(DocumentType.CUSTOM);
            document.setCustomName("my-docs");
            when(documentRepository.findByProjectIdAndDocumentTypeAndCustomName(
                    PROJECT_ID, DocumentType.CUSTOM, "my-docs"))
                    .thenReturn(Optional.of(document));
            when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(DOCUMENT_ID))
                    .thenReturn(List.of());
            when(versionMapper.toResponseList(anyList())).thenReturn(List.of());
            when(documentMapper.toDetailResponse(any(ProjectDocument.class), anyList()))
                    .thenReturn(createDetailResponse());

            ProjectDocumentDetailResponse result = service.getCustomDocument(PROJECT_ID, "my-docs");

            assertThat(result).isNotNull();
        }
    }

    // ── getDocumentVersion ──

    @Nested
    @DisplayName("getDocumentVersion")
    class GetDocumentVersionTests {

        @Test
        @DisplayName("returns specific version")
        void getDocumentVersion_returnsSpecific() {
            ProjectDocument document = createDocument(DocumentType.CLAUDE_MD);
            ProjectDocumentVersion version = createVersion(document, 3);
            ProjectDocumentVersionResponse versionResponse = createVersionResponse(3);

            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(versionRepository.findByDocumentIdAndVersionNumber(DOCUMENT_ID, 3))
                    .thenReturn(Optional.of(version));
            when(versionMapper.toResponse(version)).thenReturn(versionResponse);

            ProjectDocumentVersionResponse result = service.getDocumentVersion(DOCUMENT_ID, 3);

            assertThat(result).isNotNull();
            assertThat(result.versionNumber()).isEqualTo(3);
        }
    }

    // ── getDocumentVersions ──

    @Nested
    @DisplayName("getDocumentVersions")
    class GetDocumentVersionsTests {

        @Test
        @DisplayName("returns paginated version history")
        void getDocumentVersions_returnsPaged() {
            ProjectDocument document = createDocument(DocumentType.CLAUDE_MD);
            ProjectDocumentVersion v1 = createVersion(document, 1);
            Pageable pageable = PageRequest.of(0, 10);

            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(versionRepository.findByDocumentId(DOCUMENT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(v1)));
            when(versionMapper.toResponse(v1)).thenReturn(createVersionResponse(1));

            var result = service.getDocumentVersions(DOCUMENT_ID, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    // ── deleteDocument ──

    @Nested
    @DisplayName("deleteDocument")
    class DeleteDocumentTests {

        @Test
        @DisplayName("removes document and all versions")
        void deleteDocument_removesDocAndVersions() {
            ProjectDocument document = createDocument(DocumentType.AUDIT_MD);
            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

            service.deleteDocument(DOCUMENT_ID);

            verify(documentRepository).delete(document);
        }
    }

    // ── updateDocumentsFromSession ──

    @Nested
    @DisplayName("updateDocumentsFromSession")
    class UpdateDocumentsFromSessionTests {

        @Test
        @DisplayName("bulk updates existing documents")
        void updateDocumentsFromSession_bulkUpdate() {
            DocumentUpdate update = new DocumentUpdate(
                    DocumentType.CLAUDE_MD, "Updated by AI", "AI session update");

            ProjectDocument existingDoc = createDocument(DocumentType.CLAUDE_MD);

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            when(documentRepository.findByProjectIdAndDocumentType(PROJECT_ID, DocumentType.CLAUDE_MD))
                    .thenReturn(Optional.of(existingDoc));
            when(versionRepository.findMaxVersionNumber(DOCUMENT_ID)).thenReturn(2);
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateDocumentsFromSession(PROJECT_ID, SESSION_ID, USER_ID, List.of(update));

            ArgumentCaptor<ProjectDocumentVersion> versionCaptor =
                    ArgumentCaptor.forClass(ProjectDocumentVersion.class);
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(3);
            assertThat(versionCaptor.getValue().getAuthorType()).isEqualTo(AuthorType.AI);
            assertThat(versionCaptor.getValue().getContent()).isEqualTo("Updated by AI");

            ArgumentCaptor<ProjectDocument> docCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getCurrentContent()).isEqualTo("Updated by AI");
            assertThat(docCaptor.getValue().getLastAuthorType()).isEqualTo(AuthorType.AI);
            assertThat(docCaptor.getValue().getLastSessionId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("creates new documents for missing types")
        void updateDocumentsFromSession_createsNewDocs() {
            DocumentUpdate update = new DocumentUpdate(
                    DocumentType.ARCHITECTURE_MD, "# New Architecture", "Generated by AI");

            when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(createProject()));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            when(documentRepository.findByProjectIdAndDocumentType(PROJECT_ID, DocumentType.ARCHITECTURE_MD))
                    .thenReturn(Optional.empty());
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> {
                        ProjectDocument doc = inv.getArgument(0);
                        doc.setId(UUID.randomUUID());
                        return doc;
                    });
            when(versionRepository.save(any(ProjectDocumentVersion.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateDocumentsFromSession(PROJECT_ID, SESSION_ID, USER_ID, List.of(update));

            ArgumentCaptor<ProjectDocument> docCaptor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.ARCHITECTURE_MD);
            assertThat(docCaptor.getValue().getCurrentContent()).isEqualTo("# New Architecture");
            assertThat(docCaptor.getValue().getLastAuthorType()).isEqualTo(AuthorType.AI);

            ArgumentCaptor<ProjectDocumentVersion> versionCaptor =
                    ArgumentCaptor.forClass(ProjectDocumentVersion.class);
            verify(versionRepository).save(versionCaptor.capture());
            assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(1);
        }
    }

    // ── checkAndFlagStaleness ──

    @Nested
    @DisplayName("checkAndFlagStaleness")
    class CheckAndFlagStalenessTests {

        @Test
        @DisplayName("flags documents when sessions exceed threshold")
        void checkAndFlagStaleness_flagsOldDocs() {
            ProjectDocument doc = createDocument(DocumentType.CLAUDE_MD);
            doc.setFlagged(false);

            when(documentRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(doc));
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.checkAndFlagStaleness(PROJECT_ID, 5);

            ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().isFlagged()).isTrue();
            assertThat(captor.getValue().getFlagReason()).contains("5 sessions");
        }

        @Test
        @DisplayName("skips when under threshold")
        void checkAndFlagStaleness_skipsFreshDocs() {
            service.checkAndFlagStaleness(PROJECT_ID, 2);

            verify(documentRepository, never()).findByProjectId(any());
            verify(documentRepository, never()).save(any());
        }
    }

    // ── getFlaggedDocuments ──

    @Nested
    @DisplayName("getFlaggedDocuments")
    class GetFlaggedDocumentsTests {

        @Test
        @DisplayName("returns flagged documents only")
        void getFlaggedDocuments_returnsFlaggedOnly() {
            ProjectDocument flagged = createDocument(DocumentType.CLAUDE_MD);
            flagged.setFlagged(true);
            flagged.setFlagReason("3 sessions since update");

            when(documentRepository.findByProjectIdAndIsFlaggedTrue(PROJECT_ID))
                    .thenReturn(List.of(flagged));
            when(documentMapper.toResponseList(anyList()))
                    .thenReturn(List.of(new ProjectDocumentResponse(
                            DOCUMENT_ID, DocumentType.CLAUDE_MD, null,
                            AuthorType.HUMAN, null, true, "3 sessions since update",
                            PROJECT_ID, "Test Developer", NOW, NOW)));

            List<ProjectDocumentResponse> result = service.getFlaggedDocuments(PROJECT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).isFlagged()).isTrue();
            verify(documentRepository).findByProjectIdAndIsFlaggedTrue(PROJECT_ID);
        }
    }

    // ── clearFlag ──

    @Nested
    @DisplayName("clearFlag")
    class ClearFlagTests {

        @Test
        @DisplayName("resets flag and reason")
        void clearFlag_resetsFlag() {
            ProjectDocument document = createDocument(DocumentType.CLAUDE_MD);
            document.setFlagged(true);
            document.setFlagReason("5 sessions since update");

            when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
            when(documentRepository.save(any(ProjectDocument.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.clearFlag(DOCUMENT_ID);

            ArgumentCaptor<ProjectDocument> captor = ArgumentCaptor.forClass(ProjectDocument.class);
            verify(documentRepository).save(captor.capture());
            assertThat(captor.getValue().isFlagged()).isFalse();
            assertThat(captor.getValue().getFlagReason()).isNull();
        }
    }

    // ── getDocumentsAsMap ──

    @Nested
    @DisplayName("getDocumentsAsMap")
    class GetDocumentsAsMapTests {

        @Test
        @DisplayName("returns correct map format")
        void getDocumentsAsMap_returnsCorrectMap() {
            ProjectDocument claudeMd = createDocument(DocumentType.CLAUDE_MD);
            claudeMd.setCurrentContent("# Claude Instructions");

            ProjectDocument openApi = createDocument(DocumentType.OPENAPI_YAML);
            openApi.setId(UUID.randomUUID());
            openApi.setDocumentType(DocumentType.OPENAPI_YAML);
            openApi.setCurrentContent("openapi: 3.0.3");

            ProjectDocument custom = createDocument(DocumentType.CUSTOM);
            custom.setId(UUID.randomUUID());
            custom.setDocumentType(DocumentType.CUSTOM);
            custom.setCustomName("team-guidelines");
            custom.setCurrentContent("# Guidelines");

            when(documentRepository.findByProjectId(PROJECT_ID))
                    .thenReturn(List.of(claudeMd, openApi, custom));

            Map<String, String> result = service.getDocumentsAsMap(PROJECT_ID);

            assertThat(result).hasSize(3);
            assertThat(result).containsKey("CLAUDE_MD");
            assertThat(result.get("CLAUDE_MD")).isEqualTo("# Claude Instructions");
            assertThat(result).containsKey("OPENAPI_YAML");
            assertThat(result.get("OPENAPI_YAML")).isEqualTo("openapi: 3.0.3");
            assertThat(result).containsKey("CUSTOM:team-guidelines");
            assertThat(result.get("CUSTOM:team-guidelines")).isEqualTo("# Guidelines");
        }
    }
}
