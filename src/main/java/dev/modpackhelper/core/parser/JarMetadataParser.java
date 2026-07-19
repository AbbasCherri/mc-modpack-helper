package dev.modpackhelper.core.parser;

import dev.modpackhelper.core.model.ModInfo;
import java.util.Optional;
import java.util.jar.JarFile;

public interface JarMetadataParser {

    /**
     * Returns the parsed metadata, or empty if this parser's manifest file
     * is not present in the jar. Throws if the manifest is present but broken.
     */
    Optional<ModInfo> parse(JarFile jar) throws ParseException;
}
