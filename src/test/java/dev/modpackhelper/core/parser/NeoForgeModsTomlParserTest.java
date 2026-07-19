package dev.modpackhelper.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.model.LoaderType;
import dev.modpackhelper.core.model.ModInfo;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NeoForgeModsTomlParserTest {

    @TempDir
    Path dir;

    private final NeoForgeModsTomlParser parser = new NeoForgeModsTomlParser();

    private Optional<ModInfo> parse(Path jarPath) throws Exception {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            return parser.parse(jar);
        }
    }

    @Test
    void parsesNeoForgeToml() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "a.jar", Map.of(
                "META-INF/neoforge.mods.toml",
                JarFixtures.neoForgeToml("examplemod", "1.2.3", "[1.21.1,1.22)")));

        ModInfo info = parse(jar).orElseThrow();
        assertEquals("examplemod", info.modId());
        assertEquals("examplemod display", info.name());
        assertEquals("1.2.3", info.version());
        assertEquals(LoaderType.NEOFORGE, info.loaderType());
        assertEquals("[1.21.1,1.22)", info.minecraftVersionRange());
    }

    @Test
    void fallsBackToLegacyTomlLocation() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "b.jar", Map.of(
                "META-INF/mods.toml",
                JarFixtures.neoForgeToml("oldstyle", "0.9", "[1.20.4]")));

        ModInfo info = parse(jar).orElseThrow();
        assertEquals("oldstyle", info.modId());
        assertEquals(LoaderType.NEOFORGE, info.loaderType());
    }

    @Test
    void prefersNeoForgeTomlOverLegacy() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "c.jar", Map.of(
                "META-INF/neoforge.mods.toml", JarFixtures.neoForgeToml("newid", "2.0", "[1.21,)"),
                "META-INF/mods.toml", JarFixtures.neoForgeToml("oldid", "1.0", "[1.20,)")));

        assertEquals("newid", parse(jar).orElseThrow().modId());
    }

    @Test
    void resolvesJarVersionPlaceholderFromManifest() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "d.jar", Map.of(
                "META-INF/MANIFEST.MF",
                "Manifest-Version: 1.0\r\nImplementation-Version: 4.5.6\r\n\r\n",
                "META-INF/neoforge.mods.toml",
                JarFixtures.neoForgeToml("placeholdered", "${file.jarVersion}", "[1.21,)")));

        assertEquals("4.5.6", parse(jar).orElseThrow().version());
    }

    @Test
    void keepsPlaceholderWhenManifestHasNoVersion() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "e.jar", Map.of(
                "META-INF/neoforge.mods.toml",
                JarFixtures.neoForgeToml("placeholdered", "${file.jarVersion}", "[1.21,)")));

        assertEquals("${file.jarVersion}", parse(jar).orElseThrow().version());
    }

    @Test
    void emptyWhenNoTomlPresent() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "f.jar", Map.of("some/Class.class", "x"));
        assertTrue(parse(jar).isEmpty());
    }

    @Test
    void throwsOnBrokenToml() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "g.jar", Map.of(
                "META-INF/neoforge.mods.toml", "this is [[not valid toml"));
        assertThrows(ParseException.class, () -> parse(jar));
    }

    @Test
    void throwsWhenModIdMissing() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "h.jar", Map.of(
                "META-INF/neoforge.mods.toml", "[[mods]]\ndisplayName = \"nameless\"\n"));
        assertThrows(ParseException.class, () -> parse(jar));
    }

    @Test
    void unknownRangeWhenNoMinecraftDependency() throws Exception {
        Path jar = JarFixtures.jarWith(dir, "i.jar", Map.of(
                "META-INF/neoforge.mods.toml",
                "[[mods]]\nmodId = \"nodeps\"\nversion = \"1.0\"\n"));

        assertEquals("unknown", parse(jar).orElseThrow().minecraftVersionRange());
    }
}
