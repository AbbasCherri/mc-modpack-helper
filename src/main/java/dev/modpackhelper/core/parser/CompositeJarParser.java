package dev.modpackhelper.core.parser;

import dev.modpackhelper.core.model.ModInfo;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;

/**
 * Tries every known metadata parser against a jar. Corrupt jars, missing
 * manifests and broken manifests all end up as empty, never an exception:
 * one bad file must not break a folder scan.
 */
public class CompositeJarParser {

    private final List<JarMetadataParser> parsers;

    public CompositeJarParser() {
        this(List.of(new NeoForgeModsTomlParser()));
    }

    public CompositeJarParser(List<JarMetadataParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public Optional<ModInfo> extract(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            for (JarMetadataParser parser : parsers) {
                Optional<ModInfo> info = parser.parse(jar);
                if (info.isPresent()) {
                    return info;
                }
            }
        } catch (Exception e) {
            // fall through to empty
        }
        return Optional.empty();
    }
}
