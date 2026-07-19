package dev.modpackhelper.core.online;

import java.util.Optional;
import java.util.prefs.Preferences;

/** CurseForge API key, kept in the OS user preferences store, never in the repo. */
public final class ApiKeyStore {

    private static final String KEY = "curseforge_api_key";

    private final Preferences prefs = Preferences.userNodeForPackage(ApiKeyStore.class);

    public Optional<String> curseForgeKey() {
        String value = prefs.get(KEY, "");
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public void setCurseForgeKey(String key) {
        if (key == null || key.isBlank()) {
            prefs.remove(KEY);
        } else {
            prefs.put(KEY, key.strip());
        }
    }
}
