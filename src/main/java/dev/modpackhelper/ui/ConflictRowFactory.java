package dev.modpackhelper.ui;

import dev.modpackhelper.core.model.ConflictFlag;
import dev.modpackhelper.core.model.ModEntry;
import javafx.css.PseudoClass;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/** Colors table rows through CSS pseudo classes when a mod is flagged. */
public class ConflictRowFactory implements Callback<TableView<ModEntry>, TableRow<ModEntry>> {

    private static final PseudoClass DUPLICATE = PseudoClass.getPseudoClass("duplicate");
    private static final PseudoClass CROSS_LOADER = PseudoClass.getPseudoClass("cross-loader");

    @Override
    public TableRow<ModEntry> call(TableView<ModEntry> table) {
        return new TableRow<>() {
            @Override
            protected void updateItem(ModEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                boolean duplicate = false;
                boolean crossLoader = false;
                if (!empty && entry != null && !entry.conflicts().isEmpty()) {
                    ConflictFlag flag = entry.conflicts().getFirst();
                    duplicate = flag instanceof ConflictFlag.SameLoaderDuplicate;
                    crossLoader = flag instanceof ConflictFlag.CrossLoaderConflict;
                }
                pseudoClassStateChanged(DUPLICATE, duplicate);
                pseudoClassStateChanged(CROSS_LOADER, crossLoader);
            }
        };
    }
}
