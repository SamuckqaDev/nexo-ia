package com.nexoia.workspace.service;

import com.nexoia.workspace.dto.WorkspaceFileResponse;
import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceFileNotTextException;
import com.nexoia.workspace.exception.WorkspaceFileTooLargeException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import com.nexoia.workspace.tool.WorkspaceSearchMatch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Bounded literal search across safe text files in one authorized workspace. */
@Service
@RequiredArgsConstructor
public class WorkspaceSearchService {

    private static final int MAX_VISITED_FILES = 2_000;
    private static final int MAX_EXCERPT_CHARS = 300;

    private final WorkspacePathResolver pathResolver;
    private final WorkspaceInspectionService inspection;
    private final WorkspaceContentPolicy contentPolicy;

    public List<WorkspaceSearchMatch> search(
            Workspace workspace, String query, String relativePath, int limit) {
        Path root = pathResolver.workspaceRoot(workspace);
        Path start = relativePath == null || relativePath.isBlank()
                ? root
                : pathResolver.resolveExisting(workspace, relativePath);
        if (relativePath != null && !relativePath.isBlank()) {
            contentPolicy.requireReadable(relativePath);
            if (contentPolicy.isIgnored(root, start)) {
                throw new WorkspaceAccessDeniedException();
            }
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<WorkspaceSearchMatch> matches = new ArrayList<>();
        int[] visitedFiles = {0};
        searchPath(workspace, root, start, needle, limit, visitedFiles, matches);
        return List.copyOf(matches);
    }

    private void searchPath(
            Workspace workspace,
            Path root,
            Path path,
            String needle,
            int limit,
            int[] visitedFiles,
            List<WorkspaceSearchMatch> matches) {
        if (matches.size() >= limit
                || visitedFiles[0] >= MAX_VISITED_FILES
                || Files.isSymbolicLink(path)
                || contentPolicy.isIgnored(root, path)) {
            return;
        }
        String relative = root.equals(path) ? "" : root.relativize(path).toString().replace('\\', '/');
        if (!relative.isEmpty() && contentPolicy.isSensitive(relative)) {
            return;
        }
        if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            visitedFiles[0]++;
            searchFile(workspace, root, path, needle, limit, matches);
            return;
        }
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        for (Path child : sortedChildren(path)) {
            searchPath(workspace, root, child, needle, limit, visitedFiles, matches);
            if (matches.size() >= limit || visitedFiles[0] >= MAX_VISITED_FILES) {
                return;
            }
        }
    }

    private List<Path> sortedChildren(Path directory) {
        try (var children = Files.list(directory)) {
            return children.sorted().toList();
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException();
        }
    }

    private void searchFile(
            Workspace workspace,
            Path root,
            Path file,
            String needle,
            int limit,
            List<WorkspaceSearchMatch> matches) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        try {
            contentPolicy.requireReadable(relative);
            WorkspaceFileResponse preview = inspection.file(workspace, relative, null, null);
            String[] lines = preview.content().split("\n", -1);
            for (int index = 0; index < lines.length && matches.size() < limit; index++) {
                if (lines[index].toLowerCase(Locale.ROOT).contains(needle)) {
                    matches.add(new WorkspaceSearchMatch(
                            relative, index + 1, boundedExcerpt(lines[index])));
                }
            }
        } catch (WorkspaceAccessDeniedException | WorkspaceFileNotTextException | WorkspaceFileTooLargeException ignored) {
            // A search silently omits files that the direct read policy would refuse.
        }
    }

    private String boundedExcerpt(String value) {
        String trimmed = value.trim();
        return trimmed.length() <= MAX_EXCERPT_CHARS
                ? trimmed
                : trimmed.substring(0, MAX_EXCERPT_CHARS);
    }
}
