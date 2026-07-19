package dev.modpackhelper.core.replace;

import dev.modpackhelper.core.model.OnlineSource;

/**
 * A concrete downloadable file that could replace an installed jar.
 * Exactly one of sha1 or fingerprint is set, depending on the source.
 */
public record ReplacementCandidate(
        OnlineSource source,
        String versionLabel,
        String filename,
        String downloadUrl,
        String sha1,
        Long fingerprint) {

    public static ReplacementCandidate modrinth(String versionLabel, String filename,
            String downloadUrl, String sha1) {
        return new ReplacementCandidate(OnlineSource.MODRINTH, versionLabel, filename,
                downloadUrl, sha1, null);
    }

    public static ReplacementCandidate curseForge(String versionLabel, String filename,
            String downloadUrl, long fingerprint) {
        return new ReplacementCandidate(OnlineSource.CURSEFORGE, versionLabel, filename,
                downloadUrl, null, fingerprint);
    }
}
