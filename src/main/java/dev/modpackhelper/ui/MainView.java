package dev.modpackhelper.ui;

import dev.modpackhelper.core.model.ConflictFlag;
import dev.modpackhelper.core.model.LoaderType;
import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.ModInfo;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.model.OnlineSource;
import dev.modpackhelper.core.online.ApiKeyStore;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class MainView extends BorderPane {

    private static final DateTimeFormatter MODIFIED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final TableView<ModEntry> table = new TableView<>();
    private final Label pathLabel = new Label("No folder selected");
    private final Label statusLabel = new Label("");
    private final Button rescanButton = new Button("Rescan");
    private final CheckBox onlineCheckBox = new CheckBox("Check online");
    private final ApiKeyStore keyStore = new ApiKeyStore();

    private Path currentFolder;

    public MainView() {
        Button openButton = new Button("Open Folder...");
        openButton.setOnAction(e -> chooseFolder());
        rescanButton.setDisable(true);
        rescanButton.setOnAction(e -> scan());
        Button settingsButton = new Button("Settings...");
        settingsButton.setOnAction(e -> new SettingsDialog(keyStore).showAndWait());

        setTop(new ToolBar(openButton, rescanButton, new Separator(),
                onlineCheckBox, settingsButton, new Separator(), pathLabel));
        setCenter(buildTable());
        setBottom(statusLabel);
        statusLabel.setPadding(new Insets(4, 8, 4, 8));
    }

    private TableView<ModEntry> buildTable() {
        table.getColumns().addAll(List.of(
                column("Filename", 200, e -> e.fileInfo().filename()),
                modInfoColumn("Mod ID", 140, ModInfo::modId),
                modInfoColumn("Name", 180, ModInfo::name),
                modInfoColumn("Version", 100, ModInfo::version),
                column("Loader", 90, e -> e.modInfo()
                        .map(i -> i.loaderType() == LoaderType.NEOFORGE ? "NeoForge" : "?")
                        .orElse("?")),
                modInfoColumn("MC Version", 110, ModInfo::minecraftVersionRange),
                column("Size", 80, e -> humanSize(e.fileInfo().sizeBytes())),
                column("Modified", 130, e -> MODIFIED_FORMAT.format(e.fileInfo().lastModified())),
                column("Status", 120, MainView::statusText),
                onlineColumn("Online Name", 150, OnlineModInfo::canonicalName),
                onlineColumn("By", 100, OnlineModInfo::author),
                onlineColumn("Source", 90, i -> i.source() == OnlineSource.MODRINTH
                        ? "Modrinth" : "CurseForge"),
                onlineColumn("Latest", 110, OnlineModInfo::latestVersionName),
                onlineColumn("Update", 60, i -> i.updateAvailable() ? "yes" : "")));
        table.setRowFactory(new ConflictRowFactory());
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        table.setPlaceholder(new Label("Open a mods folder to get started"));
        return table;
    }

    private TableColumn<ModEntry, String> column(String title, int width,
            Function<ModEntry, String> value) {
        TableColumn<ModEntry, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(data -> wrap(value.apply(data.getValue())));
        return col;
    }

    private TableColumn<ModEntry, String> modInfoColumn(String title, int width,
            Function<ModInfo, String> value) {
        return column(title, width, e -> e.modInfo().map(value).orElse(""));
    }

    private TableColumn<ModEntry, String> onlineColumn(String title, int width,
            Function<OnlineModInfo, String> value) {
        return column(title, width, e -> e.onlineInfo().map(value).orElse(""));
    }

    private static ObservableValue<String> wrap(String s) {
        return new ReadOnlyObjectWrapper<>(s == null ? "" : s);
    }

    private static String statusText(ModEntry entry) {
        if (entry.conflicts().isEmpty()) {
            return entry.modInfo().isPresent() ? "" : "not parsed";
        }
        return switch (entry.conflicts().getFirst()) {
            case ConflictFlag.SameLoaderDuplicate d -> "duplicate";
            case ConflictFlag.CrossLoaderConflict c -> "loader conflict";
        };
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kib = bytes / 1024.0;
        if (kib < 1024) {
            return "%.1f KiB".formatted(kib);
        }
        return "%.1f MiB".formatted(kib / 1024.0);
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a mods folder");
        Window window = getScene() == null ? null : getScene().getWindow();
        java.io.File chosen = chooser.showDialog(window);
        if (chosen != null) {
            currentFolder = chosen.toPath();
            pathLabel.setText(currentFolder.toString());
            rescanButton.setDisable(false);
            scan();
        }
    }

    private void scan() {
        if (currentFolder == null) {
            return;
        }
        statusLabel.setText("Scanning...");
        rescanButton.setDisable(true);
        ScanTask task = new ScanTask(currentFolder);
        task.setOnSucceeded(e -> {
            List<ModEntry> entries = task.getValue();
            table.getItems().setAll(entries);
            long flagged = entries.stream().filter(en -> !en.conflicts().isEmpty()).count();
            long parsed = entries.stream().filter(en -> en.modInfo().isPresent()).count();
            statusLabel.setText("%d jars, %d parsed, %d flagged".formatted(
                    entries.size(), parsed, flagged));
            rescanButton.setDisable(false);
            if (onlineCheckBox.isSelected() && !entries.isEmpty()) {
                lookupOnline(entries);
            }
        });
        task.setOnFailed(e -> {
            statusLabel.setText("Scan failed: " + task.getException().getMessage());
            rescanButton.setDisable(false);
        });
        runInBackground(task, "mod-scan");
    }

    private void lookupOnline(List<ModEntry> entries) {
        statusLabel.setText(statusLabel.getText() + " | looking up online...");
        OnlineLookupTask task = new OnlineLookupTask(currentFolder, entries);
        task.setOnSucceeded(e -> {
            OnlineLookupTask.Result result = task.getValue();
            table.getItems().replaceAll(entry -> {
                OnlineModInfo info = result.matches().get(entry.fileInfo().path());
                return info == null ? entry : entry.withOnlineInfo(info);
            });
            String version = result.gameVersion()
                    .map(v -> "MC " + v + " detected")
                    .orElse("MC version unknown, no update check");
            statusLabel.setText("%d of %d matched online | %s".formatted(
                    result.matches().size(), entries.size(), version));
        });
        task.setOnFailed(e ->
                statusLabel.setText("Online lookup failed, local results unaffected"));
        runInBackground(task, "online-lookup");
    }

    private static void runInBackground(Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
