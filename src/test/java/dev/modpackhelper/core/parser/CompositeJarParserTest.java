package dev.modpackhelper.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CompositeJarParserTest {

    @TempDir
    Path dir;

    private final CompositeJarParser parser = new CompositeJarParser();

    @Test
    void extractsFromValidJar() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "ok.jar", Map.of(
                "META-INF/neoforge.mods.toml",
                JarFixtures.neoForgeToml("somemod", "1.0", "[1.21,)")));

        assertEquals("somemod", parser.extract(jar).orElseThrow().modId());
    }

    @Test
    void emptyForJarWithoutMetadata() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "bare.jar", Map.of("a/B.class", "x"));
        assertTrue(parser.extract(jar).isEmpty());
    }

    @Test
    void emptyForBrokenMetadata() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "broken.jar", Map.of(
                "META-INF/neoforge.mods.toml", "not [[ valid"));
        assertTrue(parser.extract(jar).isEmpty());
    }

    @Test
    void emptyForCorruptJar() throws Exception {
        Path notAJar = dir.resolve("corrupt.jar");
        Files.write(notAJar, "definitely not a zip".getBytes(StandardCharsets.UTF_8));
        assertTrue(parser.extract(notAJar).isEmpty());
    }

    @Test
    void emptyForMissingFile() {
        assertTrue(parser.extract(dir.resolve("nope.jar")).isEmpty());
    }
}
