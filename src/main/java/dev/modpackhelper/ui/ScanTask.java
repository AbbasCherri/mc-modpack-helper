package dev.modpackhelper.ui;

import dev.modpackhelper.core.conflict.DuplicateConflictDetector;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.scan.ModFolderScanner;
import java.nio.file.Path;
import java.util.List;
import javafx.concurrent.Task;

public class ScanTask extends Task<List<ModEntry>> {

    private final Path folder;

    public ScanTask(Path folder) {
        this.folder = folder;
    }

    @Override
    protected List<ModEntry> call() throws Exception {
        return DuplicateConflictDetector.annotate(new ModFolderScanner().scan(folder));
    }
}
