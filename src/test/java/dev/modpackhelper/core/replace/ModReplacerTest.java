package dev.modpackhelper.core.replace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.model.FileInfo;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.model.OnlineSource;
import dev.modpackhelper.core.online.CurseForgeClient;
import dev.modpackhelper.core.online.FakeClients.FakeCurseForge;
import dev.modpackhelper.core.online.FakeClients.FakeModrinth;
import dev.modpackhelper.core.online.ModrinthClient;
import dev.modpackhelper.core.online.Murmur2Hasher;
import dev.modpackhelper.core.scan.Sha1Hasher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModReplacerTest {

    @TempDir
    Path mods;

    private FakeModrinth modrinth;
    private FakeCurseForge curseForge;

    @BeforeEach
    void setUp() {
        modrinth = new FakeModrinth();
        curseForge = new FakeCurseForge();
    }

    private static Downloader downloaderServing(byte[] content) {
        return (url, destination) -> Files.write(destination, content);
    }

    private ModEntry entryFor(Path jar, OnlineSource source, String projectId) throws Exception {
        FileInfo file = new FileInfo(jar.getFileName().toString(), jar,
                Files.size(jar), Instant.EPOCH, Sha1Hasher.sha1Hex(jar));
        OnlineModInfo online = new OnlineModInfo(source, projectId, "Mod", "", "", "1.0", "2.0", true);
        return ModEntry.localOnly(file, Optional.empty()).withOnlineInfo(online);
    }

    @Test
    void replaceBacksUpThenSwaps() throws Exception {
        Path oldJar = mods.resolve("mod-1.0.jar");
        Files.writeString(oldJar, "old content");
        byte[] newContent = "new content".getBytes(StandardCharsets.UTF_8);
        Path tmp = mods.resolve("tmp-for-hash");
        Files.write(tmp, newContent);
        String sha1 = Sha1Hasher.sha1Hex(tmp);
        Files.delete(tmp);

        var replacer = new ModReplacer(modrinth, Optional.empty(), downloaderServing(newContent));
        ReplacementResult result = replacer.replace(oldJar,
                ReplacementCandidate.modrinth("2.0", "mod-2.0.jar", "https://x/mod.jar", sha1));

        assertTrue(result.success(), result.message());
        assertFalse(Files.exists(oldJar), "old jar should have moved to backup");
        assertEquals("old content", Files.readString(result.backupPath()));
        assertTrue(result.backupPath().toString().contains(ModReplacer.BACKUP_DIR));
        assertEquals("new content", Files.readString(mods.resolve("mod-2.0.jar")));
    }

    @Test
    void sha1MismatchAbortsBeforeTouchingOriginal() throws Exception {
        Path oldJar = mods.resolve("mod-1.0.jar");
        Files.writeString(oldJar, "old content");

        var replacer = new ModReplacer(modrinth, Optional.empty(),
                downloaderServing("corrupted".getBytes(StandardCharsets.UTF_8)));
        ReplacementResult result = replacer.replace(oldJar,
                ReplacementCandidate.modrinth("2.0", "mod-2.0.jar", "https://x/mod.jar", "deadbeef"));

        assertFalse(result.success());
        assertEquals("old content", Files.readString(oldJar));
        assertFalse(Files.exists(mods.resolve("mod-2.0.jar")));
        assertFalse(Files.exists(mods.resolve("mod-2.0.jar.part")), "partial download cleaned up");
        assertFalse(Files.exists(mods.resolve(ModReplacer.BACKUP_DIR)), "no backup for a failed replace");
    }

    @Test
    void curseForgeCandidateVerifiedByFingerprint() throws Exception {
        Path oldJar = mods.resolve("mod-1.0.jar");
        Files.writeString(oldJar, "old content");
        byte[] newContent = "cf content".getBytes(StandardCharsets.UTF_8);
        long fingerprint = Murmur2Hasher.fingerprint(newContent);

        var replacer = new ModReplacer(modrinth, Optional.of(curseForge), downloaderServing(newContent));
        ReplacementResult result = replacer.replace(oldJar,
                ReplacementCandidate.curseForge("2.0", "mod-2.0.jar", "https://x/mod.jar", fingerprint));

        assertTrue(result.success(), result.message());
        assertEquals("cf content", Files.readString(mods.resolve("mod-2.0.jar")));
    }

    @Test
    void modrinthAskedFirstThenCurseForgeFallback() throws Exception {
        Path jar = mods.resolve("mod-1.0.jar");
        Files.writeString(jar, "some jar");
        ModEntry entry = entryFor(jar, OnlineSource.MODRINTH, "proj1");
        // Modrinth knows the project but has no compatible version
        modrinth.versionsByProject.put("proj1", List.of());
        long fingerprint = Murmur2Hasher.fingerprint(jar);
        curseForge.byFingerprint.put(fingerprint,
                new CurseForgeClient.FileMatch(fingerprint, 777, 1, "mod-1.0.jar", "https://x/old.jar"));
        curseForge.filesByMod.put(777, List.of(new CurseForgeClient.ModFile(
                2, "Mod 2.0", "mod-2.0.jar", "https://x/new.jar", 42L, List.of("1.21.1", "NeoForge"))));

        var replacer = new ModReplacer(modrinth, Optional.of(curseForge), downloaderServing(new byte[0]));
        ReplacementCandidate candidate = replacer.findCandidate(entry, "1.21.1").orElseThrow();

        assertEquals(OnlineSource.CURSEFORGE, candidate.source());
        assertEquals("mod-2.0.jar", candidate.filename());
        assertTrue(modrinth.calls.contains("listVersions:proj1"), "Modrinth must be asked first");
    }

    @Test
    void modrinthCandidatePreferredWhenAvailable() throws Exception {
        Path jar = mods.resolve("mod-1.0.jar");
        Files.writeString(jar, "some jar");
        ModEntry entry = entryFor(jar, OnlineSource.MODRINTH, "proj2");
        modrinth.versionsByProject.put("proj2", List.of(new ModrinthClient.Version(
                "v2", "2.0", "release",
                List.of(new ModrinthClient.VersionFile("https://x/m.jar", "mod-2.0.jar", "abc", true)))));

        var replacer = new ModReplacer(modrinth, Optional.of(curseForge), downloaderServing(new byte[0]));
        ReplacementCandidate candidate = replacer.findCandidate(entry, "1.21.1").orElseThrow();

        assertEquals(OnlineSource.MODRINTH, candidate.source());
        assertTrue(curseForge.calls.isEmpty(), "CurseForge not needed when Modrinth has a version");
    }

    @Test
    void optedOutCurseForgeDownloadGivesNoCandidate() throws Exception {
        Path jar = mods.resolve("mod-1.0.jar");
        Files.writeString(jar, "some jar");
        ModEntry entry = entryFor(jar, OnlineSource.CURSEFORGE, "777");
        curseForge.filesByMod.put(777, List.of(new CurseForgeClient.ModFile(
                2, "Mod 2.0", "mod-2.0.jar", null, 42L, List.of("1.21.1", "NeoForge"))));

        var replacer = new ModReplacer(modrinth, Optional.of(curseForge), downloaderServing(new byte[0]));
        assertTrue(replacer.findCandidate(entry, "1.21.1").isEmpty());
    }
}
