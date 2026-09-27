package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.run.DailySeed;
import com.chris.cardgame.run.Skirmish;
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

    @Test
    void skirmishTierSelectsTierPool(@TempDir Path temp) {
        List<String> tierIds = Skirmish.load()
                .encounterFor(5, EnemyLoader.load(), new SplittableRandom(99L)).stream()
                .map(EnemyDef::id).toList();
        Map<String, Object> snap = new GameSession(temp.resolve("save.json"))
                .newBattle("KNIGHT", 99L, 5);

        assertThat(snap.get("mode")).isEqualTo("skirmish");
        assertThat(snap.get("tier")).isEqualTo(5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> foes = (List<Map<String, Object>>) snap.get("enemies");
        assertThat(foes.stream().map(f -> (String) f.get("foeId")).toList())
                .isEqualTo(tierIds);
    }

    @Test
    void skirmishDefaultTierUnchanged(@TempDir Path temp) {
        Map<String, Object> snap = new GameSession(temp.resolve("save.json"))
                .newBattle("KNIGHT", 99L, null);

        assertThat(snap.get("mode")).isEqualTo("skirmish");
        assertThat(snap).doesNotContainKey("tier");
    }

    @Test
    void skirmishBadTierIsRejected(@TempDir Path temp) {
        GameSession session = new GameSession(temp.resolve("save.json"));

        assertThatThrownBy(() -> session.newBattle("KNIGHT", 99L, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tier");
    }

    @Test
    void skirmishTierThreadsThroughHttp(@TempDir Path temp) throws Exception {
        WebServer server = new WebServer(0, new GameSession(temp.resolve("save.json")));
        server.start();
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + server.port() + "/api/new-battle"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"heroClass\":\"KNIGHT\",\"seed\":99,\"tier\":5}"))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            JsonNode snap = json.readTree(response.body());
            assertThat(snap.get("tier").asInt()).isEqualTo(5);
        } finally {
            server.stop();
        }
    }
}
