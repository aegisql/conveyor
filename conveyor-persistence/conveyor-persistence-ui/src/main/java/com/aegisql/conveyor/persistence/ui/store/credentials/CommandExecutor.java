package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

interface CommandExecutor {

    default CommandResult execute(List<String> command) {
        return execute(command, null);
    }

    CommandResult execute(List<String> command, String stdin);

    final class ProcessCommandExecutor implements CommandExecutor {
        @Override
        public CommandResult execute(List<String> command, String stdin) {
            try {
                Process process = new ProcessBuilder(command).start();
                try (OutputStream outputStream = process.getOutputStream()) {
                    if (stdin != null) {
                        outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
                    }
                }
                byte[] stdout = process.getInputStream().readAllBytes();
                byte[] stderr = process.getErrorStream().readAllBytes();
                int exitCode = process.waitFor();
                return new CommandResult(
                        exitCode,
                        new String(stdout, StandardCharsets.UTF_8),
                        new String(stderr, StandardCharsets.UTF_8)
                );
            } catch (IOException e) {
                throw new IllegalStateException("Failed to execute command: " + String.join(" ", command), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while executing command: " + String.join(" ", command), e);
            }
        }
    }
}
