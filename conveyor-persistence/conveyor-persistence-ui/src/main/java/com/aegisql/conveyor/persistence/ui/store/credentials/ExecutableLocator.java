package com.aegisql.conveyor.persistence.ui.store.credentials;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ExecutableLocator {

    private ExecutableLocator() {
    }

    static Optional<Path> find(String executableName, Path... preferredPaths) {
        for (Path preferredPath : preferredPaths) {
            if (preferredPath != null && Files.isExecutable(preferredPath)) {
                return Optional.of(preferredPath.toAbsolutePath());
            }
        }
        String path = System.getenv("PATH");
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        for (String pathEntry : path.split(File.pathSeparator)) {
            if (pathEntry == null || pathEntry.isBlank()) {
                continue;
            }
            Path directory = Path.of(pathEntry);
            for (String candidate : candidateNames(executableName)) {
                Path resolved = directory.resolve(candidate);
                if (Files.isExecutable(resolved)) {
                    return Optional.of(resolved.toAbsolutePath());
                }
            }
        }
        return Optional.empty();
    }

    private static List<String> candidateNames(String executableName) {
        List<String> names = new ArrayList<>();
        names.add(executableName);
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
        if (windows && !executableName.contains(".")) {
            String pathext = System.getenv("PATHEXT");
            if (pathext != null && !pathext.isBlank()) {
                for (String extension : pathext.split(";")) {
                    if (!extension.isBlank()) {
                        names.add(executableName + extension.toLowerCase(Locale.ROOT));
                        names.add(executableName + extension.toUpperCase(Locale.ROOT));
                    }
                }
            } else {
                names.add(executableName + ".exe");
            }
        }
        return names;
    }
}
