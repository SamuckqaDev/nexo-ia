package com.nexoia.workspace.service;

import com.nexoia.workspace.exception.WorkspaceAccessDeniedException;
import com.nexoia.workspace.exception.WorkspaceUnavailableException;
import com.nexoia.workspace.model.Workspace;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Runs a fixed, read-only Git command set in a bound workspace and returns bounded output. */
@Service
@RequiredArgsConstructor
public class WorkspaceGitReadService {

    private static final int MAX_OUTPUT_BYTES = 1_000_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WorkspacePathResolver pathResolver;
    private final WorkspaceContentPolicy contentPolicy;

    public String status(Workspace workspace) {
        return run(workspace, List.of("git", "status", "--porcelain=v1", "--branch", "--untracked-files=normal"));
    }

    public String diff(Workspace workspace, String relativePath) {
        contentPolicy.requireReadable(relativePath);
        Path root = pathResolver.workspaceRoot(workspace);
        Path file = pathResolver.resolveExisting(workspace, relativePath);
        if (contentPolicy.isIgnored(root, file)) {
            throw new WorkspaceAccessDeniedException();
        }
        return run(workspace, List.of("git", "diff", "--no-ext-diff", "--unified=3", "--", relativePath));
    }

    private String run(Workspace workspace, List<String> command) {
        Path root = pathResolver.workspaceRoot(workspace);
        if (!Files.exists(root.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
            throw new WorkspaceUnavailableException("This workspace is not a Git repository");
        }
        Process process = null;
        try {
            process = new ProcessBuilder(command)
                    .directory(root.toFile())
                    .redirectErrorStream(true)
                    .start();
            Process running = process;
            FutureTask<BoundedOutput> outputTask = new FutureTask<>(() -> readBounded(running));
            Thread.startVirtualThread(outputTask);
            boolean completed = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new WorkspaceUnavailableException("Git inspection timed out");
            }
            BoundedOutput output = outputTask.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (process.exitValue() != 0) {
                throw new WorkspaceUnavailableException("Git inspection failed");
            }
            if (output.truncated()) {
                throw new WorkspaceUnavailableException("Git inspection output exceeded its limit");
            }
            return new String(output.bytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new WorkspaceUnavailableException("Git inspection is unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WorkspaceUnavailableException("Git inspection was interrupted");
        } catch (ExecutionException | TimeoutException exception) {
            throw new WorkspaceUnavailableException("Git inspection output is unavailable");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private BoundedOutput readBounded(Process process) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8_192];
        int total = 0;
        boolean truncated = false;
        int read;
        while ((read = process.getInputStream().read(buffer)) >= 0) {
            int remaining = Math.max(0, MAX_OUTPUT_BYTES - total);
            int accepted = Math.min(read, remaining);
            if (accepted > 0) {
                output.write(buffer, 0, accepted);
                total += accepted;
            }
            if (accepted < read) {
                truncated = true;
            }
        }
        return new BoundedOutput(output.toByteArray(), truncated);
    }

    private record BoundedOutput(byte[] bytes, boolean truncated) {}
}
