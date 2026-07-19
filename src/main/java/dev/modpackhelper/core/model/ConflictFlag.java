package dev.modpackhelper.core.model;

import java.nio.file.Path;
import java.util.List;

public sealed interface ConflictFlag {

    String modId();

    List<Path> conflictingFiles();

    record SameLoaderDuplicate(String modId, List<Path> conflictingFiles) implements ConflictFlag {
    }

    record CrossLoaderConflict(String modId, List<Path> conflictingFiles) implements ConflictFlag {
    }
}
