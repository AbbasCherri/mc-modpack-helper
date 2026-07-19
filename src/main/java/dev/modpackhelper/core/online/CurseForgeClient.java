package dev.modpackhelper.core.online;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Interface so the coordinator and tests can swap in fakes. */
public interface CurseForgeClient {

    record FileMatch(long fingerprint, int modId, int fileId, String fileName, String downloadUrl) {
    }

    record Mod(int id, String name, String authors, String websiteUrl) {
    }

    record ModFile(int id, String displayName, String fileName, String downloadUrl,
            long fingerprint, List<String> gameVersions) {
    }

    /** Batched: murmur2 fingerprint of a jar to its CurseForge file. Unknown fingerprints are absent. */
    Map<Long, FileMatch> lookupByFingerprints(Collection<Long> fingerprints) throws IOException, InterruptedException;

    Map<Integer, Mod> mods(Collection<Integer> modIds) throws IOException, InterruptedException;

    /** NeoForge files of a mod for the given MC version, newest first. */
    List<ModFile> listFiles(int modId, String gameVersion) throws IOException, InterruptedException;
}
