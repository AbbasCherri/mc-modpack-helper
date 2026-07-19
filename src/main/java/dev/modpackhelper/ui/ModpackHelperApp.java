package dev.modpackhelper.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ModpackHelperApp extends Application {

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new MainView(), 1100, 650);
        scene.getStylesheets().add(
                getClass().getResource("/dev/modpackhelper/ui/style.css").toExternalForm());
        stage.setTitle("mc-modpack-helper");
        stage.setScene(scene);
        stage.show();
    }
}
