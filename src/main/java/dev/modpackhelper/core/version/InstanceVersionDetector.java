package dev.modpackhelper.core.version;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Figures out which Minecraft version a mods folder belongs to by reading the
 * launcher files that usually sit next to it. Empty result means the UI should
 * ask the user instead of guessing.
 */
public final class InstanceVersionDetector {

    private InstanceVersionDetector() {
    }

    public static Optional<String> detect(Path modsFolder) {
        Path parent = modsFolder.toAbsolutePath().getParent();
        if (parent == null) {
            return Optional.empty();
        }
        Path grandParent = parent.getParent();

        // Prism/MultiMC: <instance>/mmc-pack.json with mods under <instance>/.minecraft/mods
        for (Path dir : dirs(parent, grandParent)) {
            Optional<String> version = fromMmcPack(dir.resolve("mmc-pack.json"));
            if (version.isPresent()) {
                return version;
            }
        }
        // CurseForge app: <instance>/minecraftinstance.json next to mods
        Optional<String> version = fromMinecraftInstance(parent.resolve("minecraftinstance.json"));
        if (version.isPresent()) {
            return version;
        }
        // Vanilla layout: .minecraft/versions/<v>/<v>.json, only trustworthy if there is exactly one
        return fromVanillaVersions(parent.resolve("versions"));
    }

    private static List<Path> dirs(Path parent, Path grandParent) {
        return grandParent == null ? List.of(parent) : List.of(parent, grandParent);
    }

    private static Optional<String> fromMmcPack(Path file) {
        return parseJson(file).flatMap(root -> {
            if (!root.has("components")) {
                return Optional.empty();
            }
            for (JsonElement element : root.getAsJsonArray("components")) {
                JsonObject component = element.getAsJsonObject();
                if (component.has("uid") && "net.minecraft".equals(component.get("uid").getAsString())
                        && component.has("version")) {
                    return Optional.of(component.get("version").getAsString());
                }
            }
            return Optional.empty();
        });
    }

    private static Optional<String> fromMinecraftInstance(Path file) {
        return parseJson(file).flatMap(root -> root.has("gameVersion")
                ? Optional.of(root.get("gameVersion").getAsString())
                : Optional.empty());
    }

    private static Optional<String> fromVanillaVersions(Path versionsDir) {
        if (!Files.isDirectory(versionsDir)) {
            return Optional.empty();
        }
        List<Path> versions;
        try (Stream<Path> children = Files.list(versionsDir)) {
            versions = children.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            return Optional.empty();
        }
        if (versions.size() != 1) {
            return Optional.empty();
        }
        Path only = versions.getFirst();
        return parseJson(only.resolve(only.getFileName() + ".json"))
                .flatMap(root -> root.has("id")
                        ? Optional.of(root.get("id").getAsString())
                        : Optional.empty());
    }

    private static Optional<JsonObject> parseJson(Path file) {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(file));
            return root.isJsonObject() ? Optional.of(root.getAsJsonObject()) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
