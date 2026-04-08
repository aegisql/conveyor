package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ProcessLinuxSecretToolClient implements LinuxSecretToolClient {

    private static final String APP_ATTRIBUTE = "conveyor-persistence-ui";
    private static final String LABEL_PREFIX = "Conveyor Persistence UI";

    private final Path secretToolBinary;
    private final CommandExecutor commandExecutor;

    ProcessLinuxSecretToolClient() {
        this(resolveBinary().orElseThrow(() -> new IllegalStateException("secret-tool is not available")),
                new CommandExecutor.ProcessCommandExecutor());
    }

    ProcessLinuxSecretToolClient(Path secretToolBinary, CommandExecutor commandExecutor) {
        this.secretToolBinary = secretToolBinary.toAbsolutePath();
        this.commandExecutor = commandExecutor;
    }

    static boolean isSupported() {
        return resolveBinary().isPresent();
    }

    @Override
    public Optional<String> load(long profileId, CredentialKind credentialKind) {
        CommandResult result = commandExecutor.execute(List.of(
                secretToolBinary.toString(),
                "lookup",
                "app", APP_ATTRIBUTE,
                "profile", String.valueOf(profileId),
                "kind", credentialKind.storageKey()
        ));
        if (result.exitCode() == 0) {
            String secret = result.stdout().stripTrailing();
            return secret.isEmpty() ? Optional.empty() : Optional.of(secret);
        }
        if (result.exitCode() == 1 || result.stderrOrStdout().toLowerCase(Locale.ROOT).contains("no such secret")) {
            return Optional.empty();
        }
        throw new IllegalStateException("Failed to load credential from Linux secret service: " + result.stderrOrStdout());
    }

    @Override
    public void save(long profileId, CredentialKind credentialKind, String secret) {
        if (secret == null || secret.isBlank()) {
            delete(profileId, credentialKind);
            return;
        }
        CommandResult result = commandExecutor.execute(List.of(
                secretToolBinary.toString(),
                "store",
                "--label=" + LABEL_PREFIX + " " + profileId + " " + credentialKind.storageKey(),
                "app", APP_ATTRIBUTE,
                "profile", String.valueOf(profileId),
                "kind", credentialKind.storageKey()
        ), secret);
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Failed to save credential to Linux secret service: " + result.stderrOrStdout());
        }
    }

    @Override
    public void delete(long profileId, CredentialKind credentialKind) {
        CommandResult result = commandExecutor.execute(List.of(
                secretToolBinary.toString(),
                "clear",
                "app", APP_ATTRIBUTE,
                "profile", String.valueOf(profileId),
                "kind", credentialKind.storageKey()
        ));
        if (result.exitCode() == 0 || result.exitCode() == 1) {
            return;
        }
        throw new IllegalStateException("Failed to delete credential from Linux secret service: " + result.stderrOrStdout());
    }

    private static Optional<Path> resolveBinary() {
        return ExecutableLocator.find(
                "secret-tool",
                Path.of("/usr/bin/secret-tool"),
                Path.of("/bin/secret-tool")
        );
    }
}
