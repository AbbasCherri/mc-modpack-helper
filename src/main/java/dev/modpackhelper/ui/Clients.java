package dev.modpackhelper.ui;

import dev.modpackhelper.core.online.ApiKeyStore;
import dev.modpackhelper.core.online.CurseForgeClient;
import dev.modpackhelper.core.online.CurseForgeHttpClient;
import dev.modpackhelper.core.online.ModrinthHttpClient;
import dev.modpackhelper.core.replace.Downloader;
import dev.modpackhelper.core.replace.ModReplacer;
import java.util.Optional;

/** Builds the real network-backed objects the UI tasks use. */
final class Clients {

    private Clients() {
    }

    static Optional<CurseForgeClient> curseForge() {
        return new ApiKeyStore().curseForgeKey()
                .map(key -> (CurseForgeClient) new CurseForgeHttpClient(key));
    }

    static ModReplacer replacer() {
        return new ModReplacer(new ModrinthHttpClient(), curseForge(), Downloader.overHttp());
    }
}
