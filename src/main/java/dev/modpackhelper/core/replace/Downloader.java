package dev.modpackhelper.core.replace;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/** Interface so replacer tests can fake downloads. */
public interface Downloader {

    void download(String url, Path destination) throws IOException, InterruptedException;

    static Downloader overHttp() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return (url, destination) -> {
            HttpResponse<InputStream> response = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofMinutes(5))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("download returned HTTP " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        };
    }
}
