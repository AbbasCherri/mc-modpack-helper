package dev.modpackhelper.core.online;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.online.ModrinthClient.Project;
import dev.modpackhelper.core.online.ModrinthClient.Version;
import dev.modpackhelper.core.online.ModrinthClient.VersionMatch;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Wire-format parsing against captured response shapes, no network involved. */
class ModrinthHttpClientTest {

    @Test
    void parsesVersionFilesResponse() {
        String json = """
                {
                  "abc123": {
                    "id": "ver1", "project_id": "proj1", "version_number": "2.1.0",
                    "files": []
                  }
                }
                """;
        Map<String, VersionMatch> result = ModrinthHttpClient.parseVersionFiles(json);
        assertEquals(new VersionMatch("proj1", "ver1", "2.1.0"), result.get("abc123"));
    }

    @Test
    void parsesProjectsResponse() {
        String json = """
                [{"id": "proj1", "slug": "cool-mod", "title": "Cool Mod"}]
                """;
        Map<String, Project> result = ModrinthHttpClient.parseProjects(json);
        assertEquals("Cool Mod", result.get("proj1").title());
        assertEquals("https://modrinth.com/mod/cool-mod", result.get("proj1").url());
    }

    @Test
    void parsesVersionsResponse() {
        String json = """
                [{
                  "id": "v9", "version_number": "3.0.0", "version_type": "release",
                  "files": [
                    {"url": "https://cdn.modrinth.com/f.jar", "filename": "f.jar",
                     "primary": true, "hashes": {"sha1": "feed"}}
                  ]
                },
                {
                  "id": "v8", "version_number": "3.0.0-beta", "version_type": "beta",
                  "files": []
                }]
                """;
        List<Version> versions = ModrinthHttpClient.parseVersions(json);
        assertEquals(2, versions.size());
        assertTrue(versions.get(0).isRelease());
        assertEquals("f.jar", versions.get(0).files().getFirst().filename());
        assertEquals("feed", versions.get(0).files().getFirst().sha1());
        assertEquals(false, versions.get(1).isRelease());
    }

    @Test
    void emptyResponsesParseToEmpty() {
        assertTrue(ModrinthHttpClient.parseVersionFiles("{}").isEmpty());
        assertTrue(ModrinthHttpClient.parseProjects("[]").isEmpty());
        assertTrue(ModrinthHttpClient.parseVersions("[]").isEmpty());
    }
}
