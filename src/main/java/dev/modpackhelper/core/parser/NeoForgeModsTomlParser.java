package dev.modpackhelper.core.parser;

import dev.modpackhelper.core.model.LoaderType;
import dev.modpackhelper.core.model.ModInfo;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

public class NeoForgeModsTomlParser implements JarMetadataParser {

    private static final String NEOFORGE_TOML = "META-INF/neoforge.mods.toml";
    private static final String LEGACY_TOML = "META-INF/mods.toml";

    @Override
    public Optional<ModInfo> parse(JarFile jar) throws ParseException {
        ZipEntry entry = jar.getEntry(NEOFORGE_TOML);
        if (entry == null) {
            entry = jar.getEntry(LEGACY_TOML);
        }
        if (entry == null) {
            return Optional.empty();
        }

        TomlParseResult toml;
        try (InputStream in = jar.getInputStream(entry)) {
            toml = Toml.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ParseException("cannot read " + entry.getName(), e);
        }
        if (toml.hasErrors()) {
            throw new ParseException("invalid TOML in " + entry.getName() + ": "
                    + toml.errors().getFirst().toString());
        }

        TomlArray mods = toml.getArrayOrEmpty("mods");
        if (mods.isEmpty()) {
            throw new ParseException("no [[mods]] entry in " + entry.getName());
        }
        TomlTable mod = mods.getTable(0);
        String modId = mod.getString("modId");
        if (modId == null || modId.isBlank()) {
            throw new ParseException("missing modId in " + entry.getName());
        }
        String name = orDefault(mod.getString("displayName"), modId);
        String version = resolveVersion(mod.getString("version"), jar);
        String mcRange = minecraftVersionRange(toml, modId);

        return Optional.of(new ModInfo(modId, name, version, LoaderType.NEOFORGE, mcRange));
    }

    /**
     * mods.toml versions are often the literal placeholder "${file.jarVersion}",
     * which the loader normally substitutes at runtime from the jar manifest.
     * Do the same substitution here when possible, otherwise keep the raw value
     * visible instead of hiding it.
     */
    private String resolveVersion(String declared, JarFile jar) {
        if (declared == null || declared.isBlank()) {
            return "unknown";
        }
        if (!declared.startsWith("${")) {
            return declared;
        }
        try {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String implVersion = manifest.getMainAttributes().getValue("Implementation-Version");
                if (implVersion != null && !implVersion.isBlank()) {
                    return implVersion;
                }
            }
        } catch (IOException ignored) {
        }
        return declared;
    }

    private String minecraftVersionRange(TomlParseResult toml, String modId) {
        TomlArray deps = toml.getArrayOrEmpty("dependencies." + modId);
        for (int i = 0; i < deps.size(); i++) {
            TomlTable dep = deps.getTable(i);
            if ("minecraft".equals(dep.getString("modId"))) {
                return orDefault(dep.getString("versionRange"), "unknown");
            }
        }
        return "unknown";
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
