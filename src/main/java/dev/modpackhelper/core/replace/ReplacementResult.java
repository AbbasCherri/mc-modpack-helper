package dev.modpackhelper.core.replace;

import java.nio.file.Path;

public record ReplacementResult(boolean success, String message, Path backupPath, Path newPath) {

    static ReplacementResult ok(String message, Path backupPath, Path newPath) {
        return new ReplacementResult(true, message, backupPath, newPath);
    }

    static ReplacementResult failed(String message) {
        return new ReplacementResult(false, message, null, null);
    }
}
