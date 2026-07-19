package dev.modpackhelper.core.online;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Interface so the coordinator and tests can swap in fakes. */
public interface ModrinthClient {

    record VersionMatch(String projectId, String versionId, String versionNumber) {
    }

    record Project(String id, String slug, String title) {
        public String url() {
            return "https://modrinth.com/mod/" + slug;
        }
    }

    record VersionFile(String url, String filename, String sha1, boolean primary) {
    }

    record Version(String id, String versionNumber, String channel, List<VersionFile> files) {
        public boolean isRelease() {
            return "release".equals(channel);
        }
    }

    /** Batched: sha1 of a jar to the Modrinth version it belongs to. Unknown hashes are absent. */
    Map<String, VersionMatch> lookupByHashes(Collection<String> sha1Hashes) throws IOException, InterruptedException;

    Map<String, Project> projects(Collection<String> projectIds) throws IOException, InterruptedException;

    /** Versions of a project compatible with the given MC version, newest first. */
    List<Version> listVersions(String projectId, String gameVersion) throws IOException, InterruptedException;
}
