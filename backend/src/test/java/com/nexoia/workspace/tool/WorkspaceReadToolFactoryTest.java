package com.nexoia.workspace.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStatus;
import com.nexoia.provider.dto.WorkspaceToolScope;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceTreeEntryResponse;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceEntryType;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.service.WorkspaceAccessService;
import com.nexoia.workspace.service.WorkspaceContentPolicy;
import com.nexoia.workspace.service.WorkspaceGitReadService;
import com.nexoia.workspace.service.WorkspaceInspectionService;
import com.nexoia.workspace.service.WorkspacePathResolver;
import com.nexoia.workspace.service.WorkspaceSearchService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceReadToolFactoryTest {

    @Mock private WorkspaceAccessService access;
    @Mock private WorkspaceInspectionService inspection;
    @Mock private WorkspaceSearchService search;
    @Mock private WorkspaceGitReadService git;
    @Mock private WorkspacePathResolver pathResolver;
    @Mock private AuditService audit;

    private WorkspaceReadToolFactory factory;
    private Workspace workspace;
    private WorkspaceToolScope scope;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        workspace = Workspace.builder()
                .id(workspaceId)
                .ownerId(userId)
                .name("Nexo")
                .storageType(WorkspaceStorageType.MANAGED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .build();
        scope = new WorkspaceToolScope(
                userId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                workspaceId,
                "Nexo",
                WorkspaceAccessMode.READ_ONLY,
                true);
        lenient().when(access.accessibleWorkspace(userId, workspaceId)).thenReturn(workspace);
        lenient().when(access.lightStatus(workspace)).thenReturn(WorkspaceStatus.AVAILABLE);
        factory = new WorkspaceReadToolFactory(
                access,
                inspection,
                search,
                git,
                pathResolver,
                new WorkspaceContentPolicy(),
                audit,
                Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void exposesTheSixGovernedReadTools() {
        WorkspaceReadToolSession session = factory.open(scope, ToolExecutionObserver.NOOP, () -> false);

        assertThat(session.callbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactly(
                        WorkspaceReadToolFactory.LIST_FILES,
                        WorkspaceReadToolFactory.READ_FILE,
                        WorkspaceReadToolFactory.SEARCH,
                        WorkspaceReadToolFactory.GIT_STATUS,
                        WorkspaceReadToolFactory.GIT_DIFF,
                        WorkspaceReadToolFactory.INSPECT_PROJECT);
    }

    @Test
    void listsOnlyTheReauthorizedWorkspaceAndPersistsEvidence() {
        when(inspection.tree(eq(workspace), eq("src"), eq(10), any())).thenReturn(new WorkspaceTreeResponse(
                "src",
                List.of(new WorkspaceTreeEntryResponse(
                        "src/App.java", "App.java", WorkspaceEntryType.FILE, 12L, Instant.now())),
                List.of(),
                false,
                null));
        WorkspaceReadToolSession session = factory.open(scope, ToolExecutionObserver.NOOP, () -> false);

        String result = session.callbacks().getFirst().call("{\"path\":\"src\",\"limit\":10}");

        verify(access).accessibleWorkspace(scope.userId(), scope.workspaceId());
        assertThat(result).contains("COMPLETED").contains("src/App.java");
        assertThat(session.evidence()).extracting(evidence -> evidence.status())
                .containsExactly(ToolExecutionStatus.COMPLETED);
    }

    @Test
    void returnsLineNumberedFileContent() {
        when(inspection.file(workspace, "README.md", 2, 3)).thenReturn(new WorkspaceFileResponse(
                "README.md", "second\nthird", 2, 3, 4, 20, "abc", true));
        WorkspaceReadToolSession session = factory.open(scope, ToolExecutionObserver.NOOP, () -> false);

        String result = session.callbacks().get(1).call(
                "{\"path\":\"README.md\",\"startLine\":2,\"endLine\":3}");

        assertThat(result).contains("2 | second").contains("3 | third").contains("abc");
    }

    @Test
    void reportsContentPolicyRefusalsAsDeniedEvidence() {
        when(inspection.file(workspace, ".env", null, null))
                .thenThrow(new WorkspaceAccessDeniedException());
        WorkspaceReadToolSession session = factory.open(scope, ToolExecutionObserver.NOOP, () -> false);

        String result = session.callbacks().get(1).call("{\"path\":\".env\"}");

        assertThat(result).contains("DENIED").contains("content policy");
        assertThat(session.evidence()).extracting(ToolExecutionEvidence::status)
                .containsExactly(ToolExecutionStatus.DENIED);
    }
}
