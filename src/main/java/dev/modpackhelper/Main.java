package dev.modpackhelper;

import dev.modpackhelper.ui.ModpackHelperApp;
import javafx.application.Application;

/** Separate from the Application subclass so a plain classpath launch works. */
public class Main {
    public static void main(String[] args) {
        Application.launch(ModpackHelperApp.class, args);
    }
}
