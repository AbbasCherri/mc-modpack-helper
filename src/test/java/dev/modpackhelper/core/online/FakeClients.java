package dev.modpackhelper.core.online;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Hand-rolled fakes, enough for coordinator and replacer tests. */
public final class FakeClients {

    private FakeClients() {
    }

    public static class FakeModrinth implements ModrinthClient {
        public final Map<String, VersionMatch> byHash = new HashMap<>();
        public final Map<String, Project> projectById = new HashMap<>();
        public final Map<String, List<Version>> versionsByProject = new HashMap<>();
        public boolean failEverything = false;
        public final List<String> calls = new ArrayList<>();

        @Override
        public Map<String, VersionMatch> lookupByHashes(Collection<String> sha1Hashes) throws java.io.IOException {
            calls.add("lookupByHashes");
            failIfAsked();
            Map<String, VersionMatch> result = new HashMap<>();
            for (String hash : sha1Hashes) {
                if (byHash.containsKey(hash)) {
                    result.put(hash, byHash.get(hash));
                }
            }
            return result;
        }

        @Override
        public Map<String, Project> projects(Collection<String> projectIds) throws java.io.IOException {
            calls.add("projects");
            failIfAsked();
            Map<String, Project> result = new HashMap<>();
            for (String id : projectIds) {
                if (projectById.containsKey(id)) {
                    result.put(id, projectById.get(id));
                }
            }
            return result;
        }

        @Override
        public List<Version> listVersions(String projectId, String gameVersion) throws java.io.IOException {
            calls.add("listVersions:" + projectId);
            failIfAsked();
            return versionsByProject.getOrDefault(projectId, List.of());
        }

        private void failIfAsked() throws java.io.IOException {
            if (failEverything) {
                throw new java.io.IOException("modrinth down");
            }
        }
    }

    public static class FakeCurseForge implements CurseForgeClient {
        public final Map<Long, FileMatch> byFingerprint = new HashMap<>();
        public final Map<Integer, Mod> modById = new HashMap<>();
        public final Map<Integer, List<ModFile>> filesByMod = new HashMap<>();
        public final List<String> calls = new ArrayList<>();

        @Override
        public Map<Long, FileMatch> lookupByFingerprints(Collection<Long> fingerprints) {
            calls.add("lookupByFingerprints");
            Map<Long, FileMatch> result = new HashMap<>();
            for (Long fp : fingerprints) {
                if (byFingerprint.containsKey(fp)) {
                    result.put(fp, byFingerprint.get(fp));
                }
            }
            return result;
        }

        @Override
        public Map<Integer, Mod> mods(Collection<Integer> modIds) {
            calls.add("mods");
            Map<Integer, Mod> result = new HashMap<>();
            for (Integer id : modIds) {
                if (modById.containsKey(id)) {
                    result.put(id, modById.get(id));
                }
            }
            return result;
        }

        @Override
        public List<ModFile> listFiles(int modId, String gameVersion) {
            calls.add("listFiles:" + modId);
            return filesByMod.getOrDefault(modId, List.of());
        }
    }
}
