package dev.modpackhelper.core.model;

public record ModInfo(
        String modId,
        String name,
        String version,
        LoaderType loaderType,
        String minecraftVersionRange) {
}
