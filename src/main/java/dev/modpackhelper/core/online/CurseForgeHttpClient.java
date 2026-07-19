package dev.modpackhelper.core.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CurseForgeHttpClient implements CurseForgeClient {

    private static final int MINECRAFT_GAME_ID = 432;
    private static final int NEOFORGE_LOADER_TYPE = 6;

    private final HttpClient http;
    private final String baseUrl;
    private final String apiKey;

    public CurseForgeHttpClient(String apiKey) {
        this("https://api.curseforge.com", apiKey);
    }

    /** Base URL is swappable so tests can point at a local stub server. */
    public CurseForgeHttpClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Map<Long, FileMatch> lookupByFingerprints(Collection<Long> fingerprints)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        fingerprints.forEach(array::add);
        body.add("fingerprints", array);

        String response = send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v1/fingerprints/" + MINECRAFT_GAME_ID))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
        return parseFingerprintMatches(response);
    }

    @Override
    public Map<Integer, Mod> mods(Collection<Integer> modIds) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        modIds.forEach(array::add);
        body.add("modIds", array);

        String response = send(HttpRequest.newBuilder(URI.create(baseUrl + "/v1/mods"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())));
        return parseMods(response);
    }

    @Override
    public List<ModFile> listFiles(int modId, String gameVersion) throws IOException, InterruptedException {
        String response = send(HttpRequest.newBuilder(URI.create(
                baseUrl + "/v1/mods/" + modId + "/files?gameVersion=" + gameVersion
                        + "&modLoaderType=" + NEOFORGE_LOADER_TYPE)).GET());
        return parseFiles(response);
    }

    private String send(HttpRequest.Builder request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(
                request.header("x-api-key", apiKey)
                        .header("Accept", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("CurseForge returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    static Map<Long, FileMatch> parseFingerprintMatches(String json) {
        Map<Long, FileMatch> result = new HashMap<>();
        JsonObject data = JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("data");
        for (JsonElement element : data.getAsJsonArray("exactMatches")) {
            JsonObject file = element.getAsJsonObject().getAsJsonObject("file");
            FileMatch match = new FileMatch(
                    file.get("fileFingerprint").getAsLong(),
                    file.get("modId").getAsInt(),
                    file.get("id").getAsInt(),
                    file.get("fileName").getAsString(),
                    stringOrNull(file, "downloadUrl"));
            result.put(match.fingerprint(), match);
        }
        return result;
    }

    static Map<Integer, Mod> parseMods(String json) {
        Map<Integer, Mod> result = new HashMap<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("data")) {
            JsonObject mod = element.getAsJsonObject();
            StringBuilder authors = new StringBuilder();
            for (JsonElement author : mod.getAsJsonArray("authors")) {
                if (!authors.isEmpty()) {
                    authors.append(", ");
                }
                authors.append(author.getAsJsonObject().get("name").getAsString());
            }
            String websiteUrl = mod.has("links") && mod.get("links").isJsonObject()
                    ? stringOrNull(mod.getAsJsonObject("links"), "websiteUrl")
                    : null;
            Mod parsed = new Mod(
                    mod.get("id").getAsInt(),
                    mod.get("name").getAsString(),
                    authors.toString(),
                    websiteUrl);
            result.put(parsed.id(), parsed);
        }
        return result;
    }

    static List<ModFile> parseFiles(String json) {
        List<ModFile> result = new ArrayList<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("data")) {
            JsonObject file = element.getAsJsonObject();
            List<String> gameVersions = new ArrayList<>();
            for (JsonElement version : file.getAsJsonArray("gameVersions")) {
                gameVersions.add(version.getAsString());
            }
            result.add(new ModFile(
                    file.get("id").getAsInt(),
                    file.get("displayName").getAsString(),
                    file.get("fileName").getAsString(),
                    stringOrNull(file, "downloadUrl"),
                    file.get("fileFingerprint").getAsLong(),
                    gameVersions));
        }
        return result;
    }

    /** CurseForge sends JSON null for downloadUrl when a mod opted out of API distribution. */
    private static String stringOrNull(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }
}
