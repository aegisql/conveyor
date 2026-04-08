package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class MacOsKeychainCredentialStore implements CredentialStore {

    private static final Path SECURITY_BINARY = Path.of("/usr/bin/security");
    private static final String ACCOUNT_NAME = "conveyor-persistence-ui";

    private final CommandExecutor commandExecutor;

    MacOsKeychainCredentialStore() {
        this(new CommandExecutor.ProcessCommandExecutor());
    }

    MacOsKeychainCredentialStore(CommandExecutor commandExecutor) {
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
    }

    static boolean isSupported() {
        return Files.isExecutable(SECURITY_BINARY);
    }

    @Override
    public String displayName() {
        return "macOS Keychain";
    }

    @Override
    public Optional<String> load(long profileId, CredentialKind credentialKind) {
        CommandResult result = commandExecutor.execute(List.of(
                SECURITY_BINARY.toString(),
                "find-generic-password",
                "-a", ACCOUNT_NAME,
                "-s", serviceName(profileId, credentialKind),
                "-w"
        ));
        if (result.exitCode() == 0) {
            String secret = result.stdout().stripTrailing();
            return secret.isEmpty() ? Optional.empty() : Optional.of(secret);
        }
        if (isNotFound(result)) {
            return Optional.empty();
        }
        throw new IllegalStateException("Failed to load credential from macOS keychain: " + result.stderrOrStdout());
    }

    @Override
    public void save(long profileId, CredentialKind credentialKind, String secret) {
        if (secret == null || secret.isBlank()) {
            delete(profileId, credentialKind);
            return;
        }
        CommandResult result = commandExecutor.execute(List.of(
                SECURITY_BINARY.toString(),
                "add-generic-password",
                "-a", ACCOUNT_NAME,
                "-s", serviceName(profileId, credentialKind),
                "-w", secret,
                "-U"
        ));
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Failed to save credential to macOS keychain: " + result.stderrOrStdout());
        }
    }

    @Override
    public void delete(long profileId, CredentialKind credentialKind) {
        CommandResult result = commandExecutor.execute(List.of(
                SECURITY_BINARY.toString(),
                "delete-generic-password",
                "-a", ACCOUNT_NAME,
                "-s", serviceName(profileId, credentialKind)
        ));
        if (result.exitCode() == 0 || isNotFound(result)) {
            return;
        }
        throw new IllegalStateException("Failed to delete credential from macOS keychain: " + result.stderrOrStdout());
    }

    private String serviceName(long profileId, CredentialKind credentialKind) {
        return "com.aegisql.conveyor.persistence.ui.profile." + profileId + '.' + credentialKind.storageKey();
    }

    private boolean isNotFound(CommandResult result) {
        return result.exitCode() == 44 || result.stderrOrStdout().toLowerCase().contains("could not be found");
    }
}
