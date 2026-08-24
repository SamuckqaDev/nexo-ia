package com.nexoia.mcp.catalog.service;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Runs only fixed Docker MCP CLI operations without a shell or user-controlled executable. */
@Slf4j
@Component
public class DockerMcpCommandRunner {

    private static final int MAX_OUTPUT_CHARACTERS = 4_000_000;
    private final String executable;

    public DockerMcpCommandRunner(@Value("${nexo.mcp.docker-command:docker}") String executable) {
        this.executable = executable;
    }

    public DockerMcpCommandResult run(List<String> arguments, Duration timeout) {
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(executable);
        command.addAll(arguments);

        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Process running = process;
            CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readBounded(running));
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new TimeoutException("Docker MCP command timed out");
            }
            return new DockerMcpCommandResult(process.exitValue(), output.get(2, TimeUnit.SECONDS));
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException exception) {
            if (process != null) {
                process.destroyForcibly();
            }
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("[NEXO-BACK][MCP] Docker MCP command unavailable reason={}",
                    exception.getClass().getSimpleName());
            return new DockerMcpCommandResult(-1, "");
        }
    }

    private String readBounded(Process process) {
        StringBuilder output = new StringBuilder();
        char[] buffer = new char[8192];
        try (Reader reader = process.inputReader()) {
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                if (output.length() + read > MAX_OUTPUT_CHARACTERS) {
                    process.destroyForcibly();
                    return "";
                }
                output.append(buffer, 0, read);
            }
            return output.toString();
        } catch (IOException exception) {
            return "";
        }
    }
}
