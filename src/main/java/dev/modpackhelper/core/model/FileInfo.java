package dev.modpackhelper.core.model;

import java.nio.file.Path;
import java.time.Instant;

public record FileInfo(
        String filename,
        Path path,
        long sizeBytes,
        Instant lastModified,
        String sha1Hex) {
}
