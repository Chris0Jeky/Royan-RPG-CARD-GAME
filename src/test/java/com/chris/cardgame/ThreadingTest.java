package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import com.chris.cardgame.run.DailySeed;
import com.chris.cardgame.web.GameSession;
import com.chris.cardgame.web.WebServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThreadingTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void dailyRunUsesTodaySeed(@TempDir Path temp) {
        Map<String, Object> snap =
                new GameSession(temp.resolve("save.json")).newRun("KNIGHT", null, true);

        assertThat(snap.get("mode")).isEqualTo("run");
        assertThat(snap.get("daily")).isEqualTo(true);
        assertThat(((Number) snap.get("seed")).longValue()).isEqualTo(DailySeed.today());
    }

    @Test
    void explicitSeedBeatsDaily(@TempDir Path temp) {
        Map<String, Object> snap =
                new GameSession(temp.resolve("save.json")).newRun("KNIGHT", 12345L, true);

        assertThat(((Number) snap.get("seed")).longValue()).isEqualTo(12345L);
        assertThat(snap).doesNotContainKey("daily");
    }

    @Test
    void plainRunHasNoDailyFlag(@TempDir Path temp) {
        Map<String, Object> snap =
                new GameSession(temp.resolve("save.json")).newRun("KNIGHT", null, false);

        assertThat(snap.get("mode")).isEqualTo("run");
        assertThat(snap).doesNotContainKey("daily");
    }

    @Test
    void dailyParamThreadsThroughHttp(@TempDir Path temp) throws Exception {
        WebServer server = new WebServer(0, new GameSession(temp.resolve("save.json")));
        server.start();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + server.port() + "/api/new-run"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"heroClass\":\"RANGER\",\"daily\":true}"))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode snap = json.readTree(response.body());
            assertThat(snap.get("daily").asBoolean()).isTrue();
            assertThat(snap.get("seed").asLong()).isEqualTo(DailySeed.today());
        } finally {
            server.stop();
        }
    }
}
