package dev.modpackhelper.core.online;

import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.model.OnlineSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves scanned jars against Modrinth first, then CurseForge for whatever
 * Modrinth didn't recognize. Every network failure degrades to "no result for
 * that jar", nothing here ever throws.
 */
public class OnlineLookupCoordinator {

    private final ModrinthClient modrinth;
    private final Optional<CurseForgeClient> curseForge;

    public OnlineLookupCoordinator(ModrinthClient modrinth, Optional<CurseForgeClient> curseForge) {
        this.modrinth = modrinth;
        this.curseForge = curseForge;
    }

    /** Result is keyed by jar path. Jars found on neither platform are absent. */
    public Map<Path, OnlineModInfo> lookup(List<ModEntry> entries, Optional<String> gameVersion) {
        Map<Path, OnlineModInfo> results = new HashMap<>();
        List<ModEntry> unresolved = lookupModrinth(entries, gameVersion, results);
        if (!unresolved.isEmpty() && curseForge.isPresent()) {
            lookupCurseForge(curseForge.get(), unresolved, gameVersion, results);
        }
        return results;
    }

    /** Returns the entries Modrinth didn't match, for the CurseForge pass. */
    private List<ModEntry> lookupModrinth(List<ModEntry> entries, Optional<String> gameVersion,
            Map<Path, OnlineModInfo> results) {
        Map<String, ModEntry> bySha1 = new HashMap<>();
        for (ModEntry entry : entries) {
            if (entry.fileInfo().sha1Hex() != null) {
                bySha1.put(entry.fileInfo().sha1Hex(), entry);
            }
        }
        if (bySha1.isEmpty()) {
            return entries;
        }
        try {
            Map<String, ModrinthClient.VersionMatch> matches = modrinth.lookupByHashes(bySha1.keySet());
            Set<String> projectIds = new HashSet<>();
            matches.values().forEach(m -> projectIds.add(m.projectId()));
            Map<String, ModrinthClient.Project> projects =
                    projectIds.isEmpty() ? Map.of() : modrinth.projects(projectIds);
            Map<String, ModrinthClient.Version> latestByProject =
                    latestModrinthVersions(projectIds, gameVersion);
            buildModrinthResults(bySha1, matches, projects, latestByProject, results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            // Modrinth unreachable or answered garbage: let CurseForge try everything
            return entries;
        }
        List<ModEntry> unresolved = new ArrayList<>();
        for (ModEntry entry : entries) {
            if (!results.containsKey(entry.fileInfo().path())) {
                unresolved.add(entry);
            }
        }
        return unresolved;
    }

    private static void buildModrinthResults(Map<String, ModEntry> bySha1,
            Map<String, ModrinthClient.VersionMatch> matches,
            Map<String, ModrinthClient.Project> projects,
            Map<String, ModrinthClient.Version> latestByProject,
            Map<Path, OnlineModInfo> results) {
        for (var match : matches.entrySet()) {
            ModEntry entry = bySha1.get(match.getKey());
            ModrinthClient.VersionMatch version = match.getValue();
            ModrinthClient.Project project = projects.get(version.projectId());
            if (entry == null || project == null) {
                continue;
            }
            ModrinthClient.Version latest = latestByProject.get(version.projectId());
            results.put(entry.fileInfo().path(), new OnlineModInfo(
                    OnlineSource.MODRINTH,
                    project.id(),
                    project.title(),
                    "",
                    project.url(),
                    version.versionNumber(),
                    latest == null ? "" : latest.versionNumber(),
                    latest != null && !latest.id().equals(version.versionId())));
        }
    }

    private Map<String, ModrinthClient.Version> latestModrinthVersions(
            Set<String> projectIds, Optional<String> gameVersion) {
        Map<String, ModrinthClient.Version> latest = new HashMap<>();
        if (gameVersion.isEmpty()) {
            return latest;
        }
        for (String projectId : projectIds) {
            try {
                latest.put(projectId, pickLatest(modrinth.listVersions(projectId, gameVersion.get())));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return latest;
            } catch (Exception e) {
                // no update info for this one, the basic match still stands
            }
        }
        latest.values().removeIf(v -> v == null);
        return latest;
    }

    public static ModrinthClient.Version pickLatest(List<ModrinthClient.Version> versions) {
        return versions.stream()
                .filter(ModrinthClient.Version::isRelease)
                .findFirst()
                .orElse(versions.isEmpty() ? null : versions.getFirst());
    }

    private void lookupCurseForge(CurseForgeClient client, List<ModEntry> entries,
            Optional<String> gameVersion, Map<Path, OnlineModInfo> results) {
        Map<Long, ModEntry> byFingerprint = new HashMap<>();
        for (ModEntry entry : entries) {
            try {
                byFingerprint.put(Murmur2Hasher.fingerprint(entry.fileInfo().path()), entry);
            } catch (Exception e) {
                // unreadable file, skip it
            }
        }
        if (byFingerprint.isEmpty()) {
            return;
        }
        try {
            Map<Long, CurseForgeClient.FileMatch> matches =
                    client.lookupByFingerprints(byFingerprint.keySet());
            Set<Integer> modIds = new HashSet<>();
            matches.values().forEach(m -> modIds.add(m.modId()));
            Map<Integer, CurseForgeClient.Mod> mods = modIds.isEmpty() ? Map.of() : client.mods(modIds);
            buildCurseForgeResults(client, byFingerprint, matches, mods, gameVersion, results);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // CurseForge unreachable: Modrinth results already collected stay untouched
        }
    }

    private void buildCurseForgeResults(CurseForgeClient client,
            Map<Long, ModEntry> byFingerprint,
            Map<Long, CurseForgeClient.FileMatch> matches,
            Map<Integer, CurseForgeClient.Mod> mods,
            Optional<String> gameVersion,
            Map<Path, OnlineModInfo> results) {
        for (var match : matches.entrySet()) {
            ModEntry entry = byFingerprint.get(match.getKey());
            CurseForgeClient.FileMatch file = match.getValue();
            CurseForgeClient.Mod mod = mods.get(file.modId());
            if (entry == null || mod == null) {
                continue;
            }
            CurseForgeClient.ModFile latest = latestCurseForgeFile(client, file.modId(), gameVersion);
            results.put(entry.fileInfo().path(), new OnlineModInfo(
                    OnlineSource.CURSEFORGE,
                    Integer.toString(mod.id()),
                    mod.name(),
                    mod.authors(),
                    mod.websiteUrl() == null ? "" : mod.websiteUrl(),
                    file.fileName(),
                    latest == null ? "" : latest.fileName(),
                    latest != null && latest.id() != file.fileId()));
        }
    }

    private CurseForgeClient.ModFile latestCurseForgeFile(CurseForgeClient client,
            int modId, Optional<String> gameVersion) {
        if (gameVersion.isEmpty()) {
            return null;
        }
        try {
            List<CurseForgeClient.ModFile> files = client.listFiles(modId, gameVersion.get());
            return files.isEmpty() ? null : files.getFirst();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
