package dev.modpackhelper.core.replace;

import dev.modpackhelper.core.model.ModEntry;
import dev.modpackhelper.core.model.OnlineModInfo;
import dev.modpackhelper.core.model.OnlineSource;
import dev.modpackhelper.core.online.CurseForgeClient;
import dev.modpackhelper.core.online.ModrinthClient;
import dev.modpackhelper.core.online.Murmur2Hasher;
import dev.modpackhelper.core.online.OnlineLookupCoordinator;
import dev.modpackhelper.core.scan.Sha1Hasher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ModReplacer {

    private static final DateTimeFormatter BACKUP_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    public static final String BACKUP_DIR = ".modpack-helper-backup";

    private final ModrinthClient modrinth;
    private final Optional<CurseForgeClient> curseForge;
    private final Downloader downloader;

    public ModReplacer(ModrinthClient modrinth, Optional<CurseForgeClient> curseForge,
            Downloader downloader) {
        this.modrinth = modrinth;
        this.curseForge = curseForge;
        this.downloader = downloader;
    }

    /**
     * Best compatible file for this mod and MC version. Modrinth is asked
     * first; if it has nothing compatible the jar is fingerprinted and
     * CurseForge is asked instead, when a key is configured.
     */
    public Optional<ReplacementCandidate> findCandidate(ModEntry entry, String gameVersion) {
        OnlineModInfo info = entry.onlineInfo().orElse(null);
        if (info == null) {
            return Optional.empty();
        }
        if (info.source() == OnlineSource.MODRINTH) {
            Optional<ReplacementCandidate> fromModrinth = modrinthCandidate(info.projectId(), gameVersion);
            if (fromModrinth.isPresent()) {
                return fromModrinth;
            }
            return curseForgeCandidateByFingerprint(entry.fileInfo().path(), gameVersion);
        }
        return curseForgeCandidate(Integer.parseInt(info.projectId()), gameVersion);
    }

    private Optional<ReplacementCandidate> modrinthCandidate(String projectId, String gameVersion) {
        try {
            ModrinthClient.Version version =
                    OnlineLookupCoordinator.pickLatest(modrinth.listVersions(projectId, gameVersion));
            if (version == null) {
                return Optional.empty();
            }
            ModrinthClient.VersionFile file = version.files().stream()
                    .filter(ModrinthClient.VersionFile::primary)
                    .findFirst()
                    .orElse(version.files().isEmpty() ? null : version.files().getFirst());
            if (file == null) {
                return Optional.empty();
            }
            return Optional.of(ReplacementCandidate.modrinth(
                    version.versionNumber(), file.filename(), file.url(), file.sha1()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<ReplacementCandidate> curseForgeCandidateByFingerprint(Path jar, String gameVersion) {
        if (curseForge.isEmpty()) {
            return Optional.empty();
        }
        try {
            long fingerprint = Murmur2Hasher.fingerprint(jar);
            var matches = curseForge.get().lookupByFingerprints(List.of(fingerprint));
            CurseForgeClient.FileMatch match = matches.get(fingerprint);
            return match == null ? Optional.empty()
                    : curseForgeCandidate(match.modId(), gameVersion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Optional<ReplacementCandidate> curseForgeCandidate(int modId, String gameVersion) {
        if (curseForge.isEmpty()) {
            return Optional.empty();
        }
        try {
            List<CurseForgeClient.ModFile> files = curseForge.get().listFiles(modId, gameVersion);
            if (files.isEmpty()) {
                return Optional.empty();
            }
            CurseForgeClient.ModFile file = files.getFirst();
            if (file.downloadUrl() == null) {
                // author opted out of API distribution, nothing we can download
                return Optional.empty();
            }
            return Optional.of(ReplacementCandidate.curseForge(
                    file.displayName(), file.fileName(), file.downloadUrl(), file.fingerprint()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Download, verify, back up the old jar, then swap in the new one.
     * The original file is not touched until the download has been verified.
     */
    public ReplacementResult replace(Path oldJar, ReplacementCandidate candidate) {
        Path modsFolder = oldJar.toAbsolutePath().getParent();
        Path downloaded = modsFolder.resolve(candidate.filename() + ".part");
        try {
            downloader.download(candidate.downloadUrl(), downloaded);
            String verifyError = verify(downloaded, candidate);
            if (verifyError != null) {
                Files.deleteIfExists(downloaded);
                return ReplacementResult.failed(verifyError);
            }

            Path backupDir = modsFolder.resolve(BACKUP_DIR)
                    .resolve(BACKUP_STAMP.format(LocalDateTime.now()));
            Files.createDirectories(backupDir);
            Path backup = backupDir.resolve(oldJar.getFileName());
            Files.move(oldJar, backup);

            Path newJar = modsFolder.resolve(candidate.filename());
            Files.move(downloaded, newJar, StandardCopyOption.REPLACE_EXISTING);
            return ReplacementResult.ok(
                    "Replaced with " + candidate.versionLabel(), backup, newJar);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanup(downloaded);
            return ReplacementResult.failed("interrupted");
        } catch (Exception e) {
            cleanup(downloaded);
            return ReplacementResult.failed("Replace failed: " + e.getMessage());
        }
    }

    /** Returns an error message, or null when the download checks out. */
    private static String verify(Path downloaded, ReplacementCandidate candidate) throws IOException {
        if (candidate.sha1() != null) {
            String actual = Sha1Hasher.sha1Hex(downloaded);
            if (!candidate.sha1().equalsIgnoreCase(actual)) {
                return "Downloaded file failed sha1 check, nothing was changed";
            }
        } else if (candidate.fingerprint() != null) {
            long actual = Murmur2Hasher.fingerprint(downloaded);
            if (actual != candidate.fingerprint()) {
                return "Downloaded file failed fingerprint check, nothing was changed";
            }
        }
        return null;
    }

    private static void cleanup(Path downloaded) {
        try {
            Files.deleteIfExists(downloaded);
        } catch (IOException ignored) {
        }
    }
}
