package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import com.chris.cardgame.web.GameSession;
import com.chris.cardgame.web.WebServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebServerTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();

    private WebServer server;
    private String base;

    @BeforeEach
    void startServer() throws Exception {
        server = new WebServer(0, new GameSession());
        server.start();
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void servesHomePage() throws Exception {
        HttpResponse<String> response = get("/");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/html");
        assertThat(response.body()).contains("Royan");
        assertThat(response.body()).contains("/app.js");
    }

    @Test
    void servesStaticAssets() throws Exception {
        HttpResponse<String> js = get("/app.js");
        assertThat(js.statusCode()).isEqualTo(200);
        assertThat(js.headers().firstValue("Content-Type").orElse("")).contains("javascript");
        HttpResponse<String> css = get("/style.css");
        assertThat(css.statusCode()).isEqualTo(200);
        assertThat(css.headers().firstValue("Content-Type").orElse("")).contains("css");
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        assertThat(get("/nope").statusCode()).isEqualTo(404);
    }

    @Test
    void stateBeforeBattleOffersHeroes() throws Exception {
        JsonNode state = getJson("/api/state");
        assertThat(state.get("phase").asText()).isEqualTo("select");
        assertThat(state.get("heroes").size()).isEqualTo(3);
    }

    @Test
    void fullBattleOverHttp() throws Exception {
        JsonNode start = post("/api/new-battle", "{\"heroClass\":\"KNIGHT\",\"seed\":7}");
        assertThat(start.get("phase").asText()).isEqualTo("battle");
        assertThat(start.get("turn").asInt()).isEqualTo(1);
        assertThat(start.get("energy").asInt()).isEqualTo(3);
        assertThat(start.get("hand").size()).isEqualTo(4);
        assertThat(start.get("enemies").size()).isGreaterThan(0);

        JsonNode snap = start;
        int actions = 0;
        while (!snap.get("over").asBoolean() && actions < 400) {
            snap = stepBattle(snap);
            actions++;
        }
        assertThat(snap.get("over").asBoolean()).isTrue();
        assertThat(snap.get("phase").asText()).isEqualTo("over");
        assertThat(snap.get("log").size()).isGreaterThan(0);
        assertThat(snap.has("victory")).isTrue();
    }

    @Test
    void illegalPlayReturnsBadRequestWithLiveSnapshot() throws Exception {
        post("/api/new-battle", "{\"heroClass\":\"RANGER\",\"seed\":3}");
        HttpResponse<String> bad = postRaw("/api/play", "{\"hand\":99,\"target\":0}");
        assertThat(bad.statusCode()).isEqualTo(400);
        JsonNode body = JSON.readTree(bad.body());
        assertThat(body.get("error").asText()).contains("no such card");
        assertThat(body.get("phase").asText()).isEqualTo("battle");
        assertThat(body.get("over").asBoolean()).isFalse();
    }

    @Test
    void unknownHeroIsRejected() throws Exception {
        HttpResponse<String> bad = postRaw("/api/new-battle", "{\"heroClass\":\"PIRATE\"}");
        assertThat(bad.statusCode()).isEqualTo(400);
        JsonNode body = JSON.readTree(bad.body());
        assertThat(body.get("error").asText()).contains("unknown hero");
    }

    @Test
    void enginePackagesNeverImportWeb() throws Exception {
        Path base = Paths.get("src/main/java/com/chris/cardgame");
        if (!Files.isDirectory(base)) {
            fail("expected repo root as working directory, missing " + base);
        }
        List<String> engineDirs = List.of("model", "data", "combat", "ai", "map", "loot", "run");
        for (String dir : engineDirs) {
            try (Stream<Path> files = Files.walk(base.resolve(dir))) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    assertThat(source)
                            .as("engine file must not reference web UI: " + file)
                            .doesNotContain("cardgame.web");
                }
            }
        }
    }

    private JsonNode stepBattle(JsonNode snap) throws Exception {
        JsonNode hand = snap.get("hand");
        JsonNode play = null;
        for (JsonNode card : hand) {
            if (card.get("playable").asBoolean()) {
                play = card;
                break;
            }
        }
        if (play == null) {
            return post("/api/end-turn", "{}");
        }
        int target = -1;
        if (play.get("needsTarget").asBoolean()) {
            for (JsonNode foe : snap.get("enemies")) {
                if (foe.get("alive").asBoolean()) {
                    target = foe.get("index").asInt();
                    break;
                }
            }
            if (target < 0) {
                return post("/api/end-turn", "{}");
            }
        }
        JsonNode after = post("/api/play",
                "{\"hand\":" + play.get("index").asInt() + ",\"target\":" + target + "}");
        if (after.has("error")) {
            return post("/api/end-turn", "{}");
        }
        return after;
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(5)).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode getJson(String path) throws Exception {
        HttpResponse<String> response = get(path);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private JsonNode post(String path, String json) throws Exception {
        HttpResponse<String> response = postRaw(path, json);
        assertThat(response.statusCode()).as("POST " + path + " -> " + response.body())
                .isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> postRaw(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
