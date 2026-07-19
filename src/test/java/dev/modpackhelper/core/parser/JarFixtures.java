package dev.modpackhelper.core.parser;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds tiny jars in temp dirs so tests never need real mod files. */
public final class JarFixtures {

    private JarFixtures() {
    }

    public static Path jarWith(Path dir, String jarName, Map<String, String> entries) throws IOException {
        Path jar = dir.resolve(jarName);
        try (OutputStream out = Files.newOutputStream(jar);
                ZipOutputStream zip = new ZipOutputStream(out)) {
            for (var e : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return jar;
    }

    public static String neoForgeToml(String modId, String version, String mcRange) {
        return """
                modLoader = "javafml"
                loaderVersion = "[1,)"
                license = "MIT"

                [[mods]]
                modId = "%s"
                version = "%s"
                displayName = "%s display"

                [[dependencies.%s]]
                modId = "minecraft"
                type = "required"
                versionRange = "%s"
                ordering = "NONE"
                side = "BOTH"
                """.formatted(modId, version, modId, modId, mcRange);
    }
}
