package dev.modpackhelper.core.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.model.FileInfo;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.model.OnlineSource;
import dev.modpackhelper.core.online.FakeClients.FakeCurseForge;
import dev.modpackhelper.core.online.FakeClients.FakeModrinth;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OnlineLookupCoordinatorTest {

    @TempDir
    Path dir;

    private FakeModrinth modrinth;
    private FakeCurseForge curseForge;

    @BeforeEach
    void setUp() {
        modrinth = new FakeModrinth();
        curseForge = new FakeCurseForge();
    }

    private ModEntry entryWithContent(String filename, String sha1, String content) throws Exception {
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
        return ModEntry.localOnly(
                new FileInfo(filename, file, Files.size(file), Instant.EPOCH, sha1), Optional.empty());
    }

    @Test
    void modrinthHitSkipsCurseForge() throws Exception {
        ModEntry entry = entryWithContent("a.jar", "hash-a", "aaa");
        modrinth.byHash.put("hash-a", new ModrinthClient.VersionMatch("proj1", "v1", "1.0"));
        modrinth.projectById.put("proj1", new ModrinthClient.Project("proj1", "cool", "Cool"));

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.of(curseForge));
        Map<Path, OnlineModInfo> results = coordinator.lookup(List.of(entry), Optional.empty());

        OnlineModInfo info = results.get(entry.fileInfo().path());
        assertEquals(OnlineSource.MODRINTH, info.source());
        assertEquals("Cool", info.canonicalName());
        assertTrue(curseForge.calls.isEmpty(), "CurseForge must not be called on a full Modrinth hit");
    }

    @Test
    void modrinthMissFallsBackToCurseForge() throws Exception {
        ModEntry entry = entryWithContent("b.jar", "hash-b", "bbb");
        long fingerprint = Murmur2Hasher.fingerprint(entry.fileInfo().path());
        curseForge.byFingerprint.put(fingerprint,
                new CurseForgeClient.FileMatch(fingerprint, 777, 555, "b-1.0.jar", "https://x/b.jar"));
        curseForge.modById.put(777,
                new CurseForgeClient.Mod(777, "Bee Mod", "alice", "https://cf/bee"));

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.of(curseForge));
        Map<Path, OnlineModInfo> results = coordinator.lookup(List.of(entry), Optional.empty());

        OnlineModInfo info = results.get(entry.fileInfo().path());
        assertEquals(OnlineSource.CURSEFORGE, info.source());
        assertEquals("Bee Mod", info.canonicalName());
        assertEquals("alice", info.author());
    }

    @Test
    void noCurseForgeClientMeansMissesStayMisses() throws Exception {
        ModEntry entry = entryWithContent("c.jar", "hash-c", "ccc");

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.empty());
        assertTrue(coordinator.lookup(List.of(entry), Optional.empty()).isEmpty());
    }

    @Test
    void modrinthFailureStillTriesCurseForge() throws Exception {
        ModEntry entry = entryWithContent("d.jar", "hash-d", "ddd");
        modrinth.failEverything = true;
        long fingerprint = Murmur2Hasher.fingerprint(entry.fileInfo().path());
        curseForge.byFingerprint.put(fingerprint,
                new CurseForgeClient.FileMatch(fingerprint, 88, 99, "d.jar", null));
        curseForge.modById.put(88, new CurseForgeClient.Mod(88, "Dee", "", null));

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.of(curseForge));
        Map<Path, OnlineModInfo> results = coordinator.lookup(List.of(entry), Optional.empty());

        assertEquals(OnlineSource.CURSEFORGE, results.get(entry.fileInfo().path()).source());
    }

    @Test
    void updateFlaggedWhenNewerCompatibleVersionExists() throws Exception {
        ModEntry entry = entryWithContent("e.jar", "hash-e", "eee");
        modrinth.byHash.put("hash-e", new ModrinthClient.VersionMatch("proj2", "v-old", "1.0"));
        modrinth.projectById.put("proj2", new ModrinthClient.Project("proj2", "em", "Em"));
        modrinth.versionsByProject.put("proj2", List.of(
                new ModrinthClient.Version("v-new", "2.0", "release", List.of())));

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.of(curseForge));
        OnlineModInfo info = coordinator.lookup(List.of(entry), Optional.of("1.21.1"))
                .get(entry.fileInfo().path());

        assertTrue(info.updateAvailable());
        assertEquals("2.0", info.latestVersionName());
    }

    @Test
    void noUpdateWhenInstalledIsLatest() throws Exception {
        ModEntry entry = entryWithContent("f.jar", "hash-f", "fff");
        modrinth.byHash.put("hash-f", new ModrinthClient.VersionMatch("proj3", "v-cur", "3.0"));
        modrinth.projectById.put("proj3", new ModrinthClient.Project("proj3", "ef", "Ef"));
        modrinth.versionsByProject.put("proj3", List.of(
                new ModrinthClient.Version("v-cur", "3.0", "release", List.of())));

        var coordinator = new OnlineLookupCoordinator(modrinth, Optional.of(curseForge));
        OnlineModInfo info = coordinator.lookup(List.of(entry), Optional.of("1.21.1"))
                .get(entry.fileInfo().path());

        assertFalse(info.updateAvailable());
    }

    @Test
    void prefersReleaseChannelWhenPickingLatest() {
        var beta = new ModrinthClient.Version("vb", "2.0-beta", "beta", List.of());
        var release = new ModrinthClient.Version("vr", "1.9", "release", List.of());
        assertEquals(release, OnlineLookupCoordinator.pickLatest(List.of(beta, release)));
        assertEquals(beta, OnlineLookupCoordinator.pickLatest(List.of(beta)));
    }
}
