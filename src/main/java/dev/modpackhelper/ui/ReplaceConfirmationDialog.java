package dev.modpackhelper.ui;

import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineSource;
import dev.modpackhelper.core.replace.ModReplacer;
import dev.modpackhelper.core.replace.ReplacementCandidate;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class ReplaceConfirmationDialog extends Alert {

    public ReplaceConfirmationDialog(ModEntry entry, ReplacementCandidate candidate) {
        super(AlertType.CONFIRMATION);
        setTitle("Replace mod");
        setHeaderText("Replace " + entry.fileInfo().filename() + "?");
        String source = candidate.source() == OnlineSource.MODRINTH ? "Modrinth" : "CurseForge";
        setContentText("""
                New file: %s (%s)
                Downloaded from: %s

                The current jar is moved to %s/ inside the mods folder first, \
                so this can be undone by hand.""".formatted(
                candidate.filename(), candidate.versionLabel(), source, ModReplacer.BACKUP_DIR));
    }

    public boolean confirmed() {
        return showAndWait().filter(button -> button == ButtonType.OK).isPresent();
    }
}
