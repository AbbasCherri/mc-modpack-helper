package dev.modpackhelper.ui;

import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.online.ApiKeyStore;
import dev.modpackhelper.core.online.CurseForgeClient;
import dev.modpackhelper.core.online.CurseForgeHttpClient;
import dev.modpackhelper.core.online.ModrinthHttpClient;
import dev.modpackhelper.core.online.OnlineLookupCoordinator;
import dev.modpackhelper.core.version.InstanceVersionDetector;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.concurrent.Task;

public class OnlineLookupTask extends Task<OnlineLookupTask.Result> {

    public record Result(Map<Path, OnlineModInfo> matches, Optional<String> gameVersion) {
    }

    private final Path folder;
    private final List<ModEntry> entries;

    public OnlineLookupTask(Path folder, List<ModEntry> entries) {
        this.folder = folder;
        this.entries = List.copyOf(entries);
    }

    @Override
    protected Result call() {
        Optional<String> gameVersion = InstanceVersionDetector.detect(folder);
        Optional<CurseForgeClient> curseForge = new ApiKeyStore().curseForgeKey()
                .map(key -> (CurseForgeClient) new CurseForgeHttpClient(key));
        var coordinator = new OnlineLookupCoordinator(new ModrinthHttpClient(), curseForge);
        return new Result(coordinator.lookup(entries, gameVersion), gameVersion);
    }
}
