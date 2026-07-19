package dev.modpackhelper.core.conflict;

import dev.modpackhelper.core.model.ConflictFlag;
import dev.modpackhelper.core.model.LoaderType;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.ModInfo;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class DuplicateConflictDetector {

    private DuplicateConflictDetector() {
    }

    /** Returns the same entries with conflict flags filled in. */
    public static List<ModEntry> annotate(List<ModEntry> entries) {
        Map<String, List<ModEntry>> byModId = new HashMap<>();
        for (ModEntry entry : entries) {
            entry.modInfo().ifPresent(info ->
                    byModId.computeIfAbsent(info.modId(), k -> new ArrayList<>()).add(entry));
        }

        Map<String, ConflictFlag> flags = new HashMap<>();
        for (var group : byModId.entrySet()) {
            if (group.getValue().size() < 2) {
                continue;
            }
            String modId = group.getKey();
            List<Path> paths = group.getValue().stream()
                    .map(e -> e.fileInfo().path())
                    .toList();
            Set<LoaderType> loaders = group.getValue().stream()
                    .map(e -> e.modInfo().map(ModInfo::loaderType).orElse(LoaderType.UNKNOWN))
                    .collect(Collectors.toSet());
            flags.put(modId, loaders.size() > 1
                    ? new ConflictFlag.CrossLoaderConflict(modId, paths)
                    : new ConflictFlag.SameLoaderDuplicate(modId, paths));
        }

        return entries.stream()
                .map(entry -> entry.modInfo()
                        .map(info -> flags.get(info.modId()))
                        .map(flag -> entry.withConflicts(List.of(flag)))
                        .orElse(entry))
                .toList();
    }
}
