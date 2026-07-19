package dev.modpackhelper.core.conflict;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.modpackhelper.core.model.ConflictFlag;
import dev.modpackhelper.core.model.FileInfo;
import dev.modpackhelper.core.model.LoaderType;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.ModInfo;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DuplicateConflictDetectorTest {

    private static ModEntry entry(String filename, String modId, LoaderType loader) {
        FileInfo file = new FileInfo(filename, Path.of(filename), 1, Instant.EPOCH, null);
        Optional<ModInfo> info = modId == null
                ? Optional.empty()
                : Optional.of(new ModInfo(modId, modId, "1.0", loader, "[1.21,)"));
        return ModEntry.localOnly(file, info);
    }

    @Test
    void noConflictsForDistinctMods() {
        List<ModEntry> result = DuplicateConflictDetector.annotate(List.of(
                entry("a.jar", "moda", LoaderType.NEOFORGE),
                entry("b.jar", "modb", LoaderType.NEOFORGE),
                entry("c.jar", null, LoaderType.UNKNOWN)));

        assertTrue(result.stream().allMatch(e -> e.conflicts().isEmpty()));
    }

    @Test
    void flagsSameLoaderDuplicate() {
        List<ModEntry> result = DuplicateConflictDetector.annotate(List.of(
                entry("a-1.0.jar", "moda", LoaderType.NEOFORGE),
                entry("a-2.0.jar", "moda", LoaderType.NEOFORGE),
                entry("b.jar", "modb", LoaderType.NEOFORGE)));

        ConflictFlag flag = result.get(0).conflicts().getFirst();
        assertInstanceOf(ConflictFlag.SameLoaderDuplicate.class, flag);
        assertEquals("moda", flag.modId());
        assertEquals(2, flag.conflictingFiles().size());
        assertEquals(flag, result.get(1).conflicts().getFirst());
        assertTrue(result.get(2).conflicts().isEmpty());
    }

    @Test
    void flagsCrossLoaderConflict() {
        List<ModEntry> result = DuplicateConflictDetector.annotate(List.of(
                entry("a-neo.jar", "moda", LoaderType.NEOFORGE),
                entry("a-other.jar", "moda", LoaderType.UNKNOWN)));

        assertInstanceOf(ConflictFlag.CrossLoaderConflict.class,
                result.get(0).conflicts().getFirst());
    }

    @Test
    void unparsedEntriesNeverConflict() {
        List<ModEntry> result = DuplicateConflictDetector.annotate(List.of(
                entry("x.jar", null, LoaderType.UNKNOWN),
                entry("y.jar", null, LoaderType.UNKNOWN)));

        assertTrue(result.stream().allMatch(e -> e.conflicts().isEmpty()));
    }
}
