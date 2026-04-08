package com.aegisql.conveyor.persistence.ui.store.credentials;

record CommandResult(int exitCode, String stdout, String stderr) {

    String stderrOrStdout() {
        String stderrValue = stderr == null ? "" : stderr.strip();
        if (!stderrValue.isEmpty()) {
            return stderrValue;
        }
        return stdout == null ? "" : stdout.strip();
    }
}
