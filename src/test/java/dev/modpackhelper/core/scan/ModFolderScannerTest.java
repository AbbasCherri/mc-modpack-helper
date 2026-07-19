package dev.modpackhelper.core.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.parser.JarFixtures;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModFolderScannerTest {

    @TempDir
    Path dir;

    private final ModFolderScanner scanner = new ModFolderScanner();

    @Test
    void scansMixedFolder() throws Exception {
        JarFixtures.jarWith(dir, "good.jar", Map.of(
                "META-INF/neoforge.mods.toml",
                JarFixtures.neoForgeToml("goodmod", "1.0", "[1.21,)")));
        JarFixtures.jarWith(dir, "nometa.jar", Map.of("x/Y.class", "y"));
        Files.write(dir.resolve("corrupt.jar"), "junk".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("notes.txt"), "ignore me".getBytes(StandardCharsets.UTF_8));

        List<ModEntry> entries = scanner.scan(dir);

        assertEquals(3, entries.size());
        assertEquals(List.of("corrupt.jar", "good.jar", "nometa.jar"),
                entries.stream().map(e -> e.fileInfo().filename()).toList());

        ModEntry good = entries.get(1);
        assertEquals("goodmod", good.modInfo().orElseThrow().modId());
        assertNotNull(good.fileInfo().sha1Hex());
        assertTrue(good.fileInfo().sizeBytes() > 0);

        assertTrue(entries.get(0).modInfo().isEmpty());
        assertTrue(entries.get(2).modInfo().isEmpty());
    }

    @Test
    void sha1MatchesJdkDigest() throws Exception {
        Path file = dir.resolve("f.bin");
        Files.write(file, new byte[] {1, 2, 3});
        assertEquals("7037807198c22a7d2b0807371d763779a84fdfcf", Sha1Hasher.sha1Hex(file));
    }

    @Test
    void emptyFolderGivesEmptyList() throws Exception {
        assertTrue(scanner.scan(dir).isEmpty());
    }
}
