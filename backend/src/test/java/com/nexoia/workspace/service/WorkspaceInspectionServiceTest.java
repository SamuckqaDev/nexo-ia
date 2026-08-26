package com.nexoia.workspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.nexoia.workspace.config.WorkspaceProperties;
import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.dto.WorkspaceGitSummary;
import com.nexoia.workspace.dto.WorkspaceTreeResponse;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceFileNotTextException;
import com.nexoia.workspace.exception.WorkspaceFileTooLargeException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceEntryType;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.tool.WorkspaceSearchMatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceInspectionServiceTest {

    @TempDir
    Path importRoot;

    @TempDir
    Path managed;

    private WorkspaceInspectionService inspection;
    private WorkspaceSearchService search;
    private Path project;

    @BeforeEach
    void setUp() throws IOException {
        project = importRoot.resolve("project");
        Files.createDirectories(project.resolve("src"));
        Files.writeString(project.resolve("pom.xml"), "<project/>");
        Files.writeString(project.resolve("README.md"), "hello\nworld\n");
        Files.writeString(project.resolve("src/App.java"), "class App {}\n");
        Files.createDirectories(project.resolve("node_modules/left-pad"));
        WorkspaceProperties properties = new WorkspaceProperties(
                managed.toString(), importRoot.toString(), managed.resolve("artifacts").toString(),
                64, 500, 100, Duration.ofMinutes(15), Duration.ofMinutes(10));
        WorkspacePathResolver pathResolver = new WorkspacePathResolver(properties);
        WorkspaceContentPolicy contentPolicy = new WorkspaceContentPolicy();
        inspection = new WorkspaceInspectionService(properties, pathResolver, contentPolicy);
        search = new WorkspaceSearchService(pathResolver, inspection, contentPolicy);
    }

    private Workspace workspace() {
        return Workspace.builder()
                .id(UUID.randomUUID()).ownerId(UUID.randomUUID()).name("proj")
                .storageType(WorkspaceStorageType.MOUNTED)
                .accessMode(WorkspaceAccessMode.READ_ONLY)
                .relativePath("project")
                .build();
    }

    @Test
    void listsRootWithDirectoriesFirstAndIgnoresHeavyFolders() {
        WorkspaceTreeResponse tree = inspection.tree(workspace(), "", null, null);

        assertThat(tree.entries()).extracting(entry -> entry.name())
                .containsExactly("src", "README.md", "pom.xml");
        assertThat(tree.entries().getFirst().type()).isEqualTo(WorkspaceEntryType.DIRECTORY);
        assertThat(tree.omissions()).anyMatch(omission -> omission.name().equals("node_modules")
                && omission.reason().equals("ignored"));
    }

    @Test
    void paginatesWithCursor() {
        WorkspaceTreeResponse first = inspection.tree(workspace(), "", 1, null);
        assertThat(first.entries()).hasSize(1);
        assertThat(first.truncated()).isTrue();
        assertThat(first.nextCursor()).isNotNull();

        WorkspaceTreeResponse second = inspection.tree(workspace(), "", 1, first.nextCursor());
        assertThat(second.entries()).hasSize(1);
        assertThat(second.entries().getFirst().name()).isNotEqualTo(first.entries().getFirst().name());
    }

    @Test
    void fingerprintIsStableAndChangesWithStructure() throws IOException {
        String before = inspection.fingerprint(project);
        assertThat(inspection.fingerprint(project)).isEqualTo(before);

        Files.writeString(project.resolve("src/Added.java"), "class Added {}\n");
        assertThat(inspection.fingerprint(project)).isNotEqualTo(before);
    }

    @Test
    void readsGitBranchAndHead() throws IOException {
        Files.createDirectories(project.resolve(".git/refs/heads"));
        Files.writeString(project.resolve(".git/HEAD"), "ref: refs/heads/main\n");
        Files.writeString(project.resolve(".git/refs/heads/main"), "abc123def456\n");

        Optional<WorkspaceGitSummary> git = inspection.gitSummary(project);

        assertThat(git).isPresent();
        assertThat(git.get().branch()).isEqualTo("main");
        assertThat(git.get().head()).isEqualTo("abc123def456");
        assertThat(git.get().detached()).isFalse();
    }

    @Test
    void detectsStackFromManifests() {
        assertThat(inspection.detectStack(project)).contains("maven");
    }

    @Test
    void readsBoundedTextFileWithLineRange() {
        WorkspaceFileResponse file = inspection.file(workspace(), "README.md", 1, 1);

        assertThat(file.content()).isEqualTo("hello");
        assertThat(file.totalLines()).isEqualTo(3);
        assertThat(file.truncated()).isTrue();
        assertThat(file.sha256()).isNotBlank();
    }

    @Test
    void rejectsBinaryFile() throws IOException {
        Files.write(project.resolve("bin.dat"), new byte[] {1, 2, 0, 3, 4});

        assertThatExceptionOfType(WorkspaceFileNotTextException.class)
                .isThrownBy(() -> inspection.file(workspace(), "bin.dat", null, null));
    }

    @Test
    void rejectsFileOverByteLimit() throws IOException {
        Files.writeString(project.resolve("big.txt"), "x".repeat(200));

        assertThatExceptionOfType(WorkspaceFileTooLargeException.class)
                .isThrownBy(() -> inspection.file(workspace(), "big.txt", null, null));
    }

    @Test
    void rejectsCredentialBearingFiles() throws IOException {
        Files.writeString(project.resolve(".env"), "TOKEN=secret");

        assertThatExceptionOfType(WorkspaceAccessDeniedException.class)
                .isThrownBy(() -> inspection.file(workspace(), ".env", null, null));
        assertThat(inspection.tree(workspace(), "", null, null).entries())
                .extracting(entry -> entry.name())
                .doesNotContain(".env");
    }

    @Test
    void rejectsDirectAccessToIgnoredDependencyDirectories() {
        assertThatExceptionOfType(WorkspaceAccessDeniedException.class)
                .isThrownBy(() -> inspection.tree(workspace(), "node_modules", null, null));
    }

    @Test
    void searchesSafeFilesWithoutEnteringSensitiveOrIgnoredPaths() throws IOException {
        Files.writeString(project.resolve("node_modules/left-pad/index.js"), "hidden needle");
        Files.writeString(project.resolve(".env.local"), "TOKEN=hidden needle");
        Files.writeString(project.resolve("src/App.java"), "class App { String needle; }\n");

        List<WorkspaceSearchMatch> matches = search.search(workspace(), "needle", null, 10);

        assertThat(matches).extracting(WorkspaceSearchMatch::path)
                .containsExactly("src/App.java");
        assertThatExceptionOfType(WorkspaceAccessDeniedException.class)
                .isThrownBy(() -> search.search(workspace(), "needle", "node_modules", 10));
    }

    @Test
    void fingerprintChangesWhenSameSizeFileIsModified() throws Exception {
        Path file = project.resolve("README.md");
        String before = inspection.fingerprint(project);
        Thread.sleep(5L);
        Files.writeString(file, "HELLO\nworld\n");

        assertThat(inspection.fingerprint(project)).isNotEqualTo(before);
    }

    @Test
    void detectsChangeThroughStatus() {
        Workspace workspace = workspace();
        String fingerprint = inspection.fingerprint(project);
        workspace.recordScan(fingerprint, null, java.time.Instant.now());

        assertThat(inspection.status(workspace).status().name()).isEqualTo("AVAILABLE");

        Workspace stale = workspace();
        stale.recordScan("deadbeef", null, java.time.Instant.now());
        assertThat(inspection.status(stale).status().name()).isEqualTo("CHANGED");
    }
}
