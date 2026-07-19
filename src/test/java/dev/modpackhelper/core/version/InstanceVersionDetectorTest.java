package dev.modpackhelper.core.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceVersionDetectorTest {

    @TempDir
    Path dir;

    @Test
    void readsPrismLayout() throws Exception {
        // <instance>/mmc-pack.json, mods under <instance>/.minecraft/mods
        Files.writeString(dir.resolve("mmc-pack.json"), """
                {"components": [
                  {"uid": "net.minecraft", "version": "1.21.1"},
                  {"uid": "net.neoforged", "version": "21.1.77"}
                ], "formatVersion": 1}
                """);
        Path mods = Files.createDirectories(dir.resolve(".minecraft/mods"));

        assertEquals(Optional.of("1.21.1"), InstanceVersionDetector.detect(mods));
    }

    @Test
    void readsCurseForgeAppLayout() throws Exception {
        Files.writeString(dir.resolve("minecraftinstance.json"), """
                {"gameVersion": "1.20.4", "name": "My Pack"}
                """);
        Path mods = Files.createDirectories(dir.resolve("mods"));

        assertEquals(Optional.of("1.20.4"), InstanceVersionDetector.detect(mods));
    }

    @Test
    void readsVanillaLayoutWithSingleVersion() throws Exception {
        Path versionDir = Files.createDirectories(dir.resolve("versions/1.21.4"));
        Files.writeString(versionDir.resolve("1.21.4.json"), """
                {"id": "1.21.4", "type": "release"}
                """);
        Path mods = Files.createDirectories(dir.resolve("mods"));

        assertEquals(Optional.of("1.21.4"), InstanceVersionDetector.detect(mods));
    }

    @Test
    void ambiguousVanillaVersionsGiveEmpty() throws Exception {
        Files.createDirectories(dir.resolve("versions/1.21.1"));
        Files.createDirectories(dir.resolve("versions/1.20.4"));
        Path mods = Files.createDirectories(dir.resolve("mods"));

        assertTrue(InstanceVersionDetector.detect(mods).isEmpty());
    }

    @Test
    void unrecognizedFolderGivesEmpty() throws Exception {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        assertTrue(InstanceVersionDetector.detect(mods).isEmpty());
    }

    @Test
    void brokenJsonGivesEmpty() throws Exception {
        Files.writeString(dir.resolve("minecraftinstance.json"), "{not json");
        Path mods = Files.createDirectories(dir.resolve("mods"));
        assertTrue(InstanceVersionDetector.detect(mods).isEmpty());
    }
}
