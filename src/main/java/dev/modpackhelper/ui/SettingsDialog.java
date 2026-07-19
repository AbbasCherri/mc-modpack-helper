package dev.modpackhelper.ui;

import dev.modpackhelper.core.online.ApiKeyStore;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

/** Just the CurseForge API key for now. */
public class SettingsDialog extends Dialog<Void> {

    public SettingsDialog(ApiKeyStore keyStore) {
        setTitle("Settings");
        PasswordField keyField = new PasswordField();
        keyField.setPromptText("CurseForge API key");
        keyField.setText(keyStore.curseForgeKey().orElse(""));

        VBox content = new VBox(8,
                new Label("CurseForge API key (leave empty to skip CurseForge lookups):"),
                keyField);
        content.setPadding(new Insets(12));
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                keyStore.setCurseForgeKey(keyField.getText());
            }
            return null;
        });
    }
}
