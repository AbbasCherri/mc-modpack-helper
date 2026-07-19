package dev.modpackhelper.core.model;

import java.util.List;
import java.util.Optional;

public record ModEntry(
        FileInfo fileInfo,
        Optional<ModInfo> modInfo,
        Optional<OnlineModInfo> onlineInfo,
        List<ConflictFlag> conflicts) {

    public static ModEntry localOnly(FileInfo fileInfo, Optional<ModInfo> modInfo) {
        return new ModEntry(fileInfo, modInfo, Optional.empty(), List.of());
    }

    public ModEntry withOnlineInfo(OnlineModInfo info) {
        return new ModEntry(fileInfo, modInfo, Optional.of(info), conflicts);
    }

    public ModEntry withConflicts(List<ConflictFlag> newConflicts) {
        return new ModEntry(fileInfo, modInfo, onlineInfo, List.copyOf(newConflicts));
    }
}
