package dev.modpackhelper.core.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModrinthHttpClient implements ModrinthClient {

    public static final String USER_AGENT =
            "mc-modpack-helper/0.1 (github.com/AbbasCherri/mc-modpack-helper)";

    private final HttpClient http;
    private final String baseUrl;

    public ModrinthHttpClient() {
        this("https://api.modrinth.com");
    }

    /** Base URL is swappable so tests can point at a local stub server. */
    public ModrinthHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Map<String, VersionMatch> lookupByHashes(Collection<String> sha1Hashes)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonArray hashes = new JsonArray();
        sha1Hashes.forEach(hashes::add);
        body.add("hashes", hashes);
        body.addProperty("algorithm", "sha1");

        String response = send(HttpRequest.newBuilder(URI.create(baseUrl + "/v2/version_files"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
        return parseVersionFiles(response);
    }

    @Override
    public Map<String, Project> projects(Collection<String> projectIds)
            throws IOException, InterruptedException {
        JsonArray ids = new JsonArray();
        projectIds.forEach(ids::add);
        String response = send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v2/projects?ids=" + encode(ids.toString()))).GET());
        return parseProjects(response);
    }

    @Override
    public List<Version> listVersions(String projectId, String gameVersion)
            throws IOException, InterruptedException {
        String query = "?loaders=" + encode("[\"neoforge\"]")
                + "&game_versions=" + encode("[\"" + gameVersion + "\"]");
        String response = send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v2/project/" + projectId + "/version" + query)).GET());
        return parseVersions(response);
    }

    private String send(HttpRequest.Builder request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                request.header("User-Agent", USER_AGENT).timeout(Duration.ofSeconds(15)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Modrinth returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static Map<String, VersionMatch> parseVersionFiles(String json) {
        Map<String, VersionMatch> result = new HashMap<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        for (String hash : root.keySet()) {
            JsonObject version = root.getAsJsonObject(hash);
            result.put(hash, new VersionMatch(
                    version.get("project_id").getAsString(),
                    version.get("id").getAsString(),
                    version.get("version_number").getAsString()));
        }
        return result;
    }

    static Map<String, Project> parseProjects(String json) {
        Map<String, Project> result = new HashMap<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject project = element.getAsJsonObject();
            Project parsed = new Project(
                    project.get("id").getAsString(),
                    project.get("slug").getAsString(),
                    project.get("title").getAsString());
            result.put(parsed.id(), parsed);
        }
        return result;
    }

    static List<Version> parseVersions(String json) {
        List<Version> result = new ArrayList<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject version = element.getAsJsonObject();
            List<VersionFile> files = new ArrayList<>();
            for (JsonElement fileElement : version.getAsJsonArray("files")) {
                JsonObject file = fileElement.getAsJsonObject();
                files.add(new VersionFile(
                        file.get("url").getAsString(),
                        file.get("filename").getAsString(),
                        file.getAsJsonObject("hashes").get("sha1").getAsString(),
                        file.get("primary").getAsBoolean()));
            }
            result.add(new Version(
                    version.get("id").getAsString(),
                    version.get("version_number").getAsString(),
                    version.get("version_type").getAsString(),
                    files));
        }
        return result;
    }
}
