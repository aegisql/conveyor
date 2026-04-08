package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class PowerShellWindowsDataProtector implements WindowsDataProtector {

    private static final String PROTECT_SCRIPT = "$ErrorActionPreference = 'Stop'; "
            + "$plain = [Console]::In.ReadToEnd(); "
            + "$secure = ConvertTo-SecureString $plain -AsPlainText -Force; "
            + "$protected = ConvertFrom-SecureString $secure; "
            + "[Console]::Out.Write($protected)";

    private static final String UNPROTECT_SCRIPT = "$ErrorActionPreference = 'Stop'; "
            + "$protected = [Console]::In.ReadToEnd().Trim(); "
            + "if ([string]::IsNullOrWhiteSpace($protected)) { exit 3 }; "
            + "$secure = ConvertTo-SecureString $protected; "
            + "$ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure); "
            + "try { [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } "
            + "finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }";

    private final Path powerShellBinary;
    private final CommandExecutor commandExecutor;

    PowerShellWindowsDataProtector() {
        this(resolveBinary().orElseThrow(() -> new IllegalStateException("PowerShell is not available")),
                new CommandExecutor.ProcessCommandExecutor());
    }

    PowerShellWindowsDataProtector(Path powerShellBinary, CommandExecutor commandExecutor) {
        this.powerShellBinary = powerShellBinary.toAbsolutePath();
        this.commandExecutor = commandExecutor;
    }

    static boolean isSupported() {
        return resolveBinary().isPresent();
    }

    @Override
    public String protect(String secret) {
        CommandResult result = commandExecutor.execute(List.of(
                powerShellBinary.toString(),
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                PROTECT_SCRIPT
        ), secret);
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Failed to protect credential with Windows DPAPI: " + result.stderrOrStdout());
        }
        return result.stdout().stripTrailing();
    }

    @Override
    public String unprotect(String protectedSecret) {
        CommandResult result = commandExecutor.execute(List.of(
                powerShellBinary.toString(),
                "-NoLogo",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                UNPROTECT_SCRIPT
        ), protectedSecret);
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Failed to read Windows-protected credential: " + result.stderrOrStdout());
        }
        return result.stdout().stripTrailing();
    }

    private static Optional<Path> resolveBinary() {
        String systemRoot = System.getenv("SystemRoot");
        Path systemPath = systemRoot == null || systemRoot.isBlank()
                ? null
                : Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        return ExecutableLocator.find("powershell.exe", systemPath);
    }
}
