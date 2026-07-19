package dev.modpackhelper.core.scan;

import dev.modpackhelper.core.model.FileInfo;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.parser.CompositeJarParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class ModFolderScanner {

    private final CompositeJarParser parser;

    public ModFolderScanner() {
        this(new CompositeJarParser());
    }

    public ModFolderScanner(CompositeJarParser parser) {
        this.parser = parser;
    }

    public List<ModEntry> scan(Path modsFolder) throws IOException {
        List<Path> jars;
        try (Stream<Path> files = Files.list(modsFolder)) {
            jars = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
        List<ModEntry> entries = new ArrayList<>(jars.size());
        for (Path jar : jars) {
            toEntry(jar).ifPresent(entries::add);
        }
        return entries;
    }

    private Optional<ModEntry> toEntry(Path jar) {
        FileInfo fileInfo;
        try {
            String sha1 = null;
            try {
                sha1 = Sha1Hasher.sha1Hex(jar);
            } catch (IOException ignored) {
                // unreadable content, keep the entry with what stat gives us
            }
            fileInfo = new FileInfo(
                    jar.getFileName().toString(),
                    jar,
                    Files.size(jar),
                    Files.getLastModifiedTime(jar).toInstant(),
                    sha1);
        } catch (IOException e) {
            // cannot even stat the file, nothing useful to show
            return Optional.empty();
        }
        return Optional.of(ModEntry.localOnly(fileInfo, parser.extract(jar)));
    }
}
