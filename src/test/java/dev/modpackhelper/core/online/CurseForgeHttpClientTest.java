package dev.modpackhelper.core.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.online.CurseForgeClient.FileMatch;
import dev.modpackhelper.core.online.CurseForgeClient.Mod;
import dev.modpackhelper.core.online.CurseForgeClient.ModFile;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wire-format parsing against captured response shapes, no network involved. */
class CurseForgeHttpClientTest {

    @Test
    void parsesFingerprintMatches() {
        String json = """
                {"data": {"exactMatches": [
                  {"id": 999, "file": {
                    "id": 555, "modId": 777, "fileName": "cool-1.0.jar",
                    "fileFingerprint": 123456789,
                    "downloadUrl": "https://edge.forgecdn.net/cool-1.0.jar"
                  }}
                ], "unmatchedFingerprints": [42]}}
                """;
        Map<Long, FileMatch> result = CurseForgeHttpClient.parseFingerprintMatches(json);
        FileMatch match = result.get(123456789L);
        assertEquals(777, match.modId());
        assertEquals(555, match.fileId());
        assertEquals("cool-1.0.jar", match.fileName());
    }

    @Test
    void parsesModsWithAuthorsAndOptedOutDownloads() {
        String json = """
                {"data": [{
                  "id": 777, "name": "Cool Mod",
                  "authors": [{"name": "alice"}, {"name": "bob"}],
                  "links": {"websiteUrl": "https://www.curseforge.com/minecraft/mc-mods/cool"}
                }]}
                """;
        Map<Integer, Mod> result = CurseForgeHttpClient.parseMods(json);
        assertEquals("alice, bob", result.get(777).authors());
        assertEquals("https://www.curseforge.com/minecraft/mc-mods/cool", result.get(777).websiteUrl());
    }

    @Test
    void parsesFilesAndNullDownloadUrl() {
        String json = """
                {"data": [{
                  "id": 555, "displayName": "Cool 2.0", "fileName": "cool-2.0.jar",
                  "downloadUrl": null, "fileFingerprint": 987,
                  "gameVersions": ["1.21.1", "NeoForge"]
                }]}
                """;
        List<ModFile> files = CurseForgeHttpClient.parseFiles(json);
        assertEquals("cool-2.0.jar", files.getFirst().fileName());
        assertNull(files.getFirst().downloadUrl());
        assertTrue(files.getFirst().gameVersions().contains("NeoForge"));
    }
}
