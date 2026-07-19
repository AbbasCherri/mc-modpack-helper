package dev.modpackhelper.core.model;

public record OnlineModInfo(
        OnlineSource source,
        String projectId,
        String canonicalName,
        String author,
        String projectUrl,
        String installedVersionName,
        String latestVersionName,
        boolean updateAvailable) {
}
