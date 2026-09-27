package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
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
import org.junit.jupiter.api.io.TempDir;

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
    void servesAudioEngine() throws Exception {
        HttpResponse<String> js = get("/audio.js");
        assertThat(js.statusCode()).isEqualTo(200);
        assertThat(js.headers().firstValue("Content-Type").orElse("")).contains("javascript");
        assertThat(js.body()).contains("RoyanAudio");
    }

    @Test
    void servesArtIcons() throws Exception {
        HttpResponse<String> svg = get("/art/rat.svg");
        assertThat(svg.statusCode()).isEqualTo(200);
        assertThat(svg.headers().firstValue("Content-Type").orElse("")).contains("svg");
        assertThat(svg.body()).contains("<svg");
        assertThat(get("/art/no-such-icon.svg").statusCode()).isEqualTo(404);
        assertThat(get("/art/rat.png").statusCode()).isEqualTo(404);
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
    void enginePackagesNeverImportUi() throws Exception {
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
                            .as("engine file must not reference UI packages: " + file)
                            .doesNotContain("cardgame.web").doesNotContain("cardgame.cli");
                }
            }
        }
    }

    @Test
    void fullRunOverHttp(@TempDir Path temp) throws Exception {
        Path save = temp.resolve("save.json");
        restartWithSave(save);
        JsonNode snap = post("/api/new-run", "{\"heroClass\":\"KNIGHT\",\"seed\":11}");
        assertThat(snap.get("phase").asText()).isEqualTo("run");
        assertThat(snap.get("screen").asText()).isEqualTo("map");
        int actions = 0;
        while (!snap.get("over").asBoolean() && actions < 900) {
            snap = stepRun(snap);
            actions++;
        }
        assertThat(snap.get("over").asBoolean()).isTrue();
        assertThat(snap.get("phase").asText()).isEqualTo("over");
        assertThat(snap.has("victory")).isTrue();
        assertThat(snap.get("log").size()).isGreaterThan(0);
        assertThat(Files.exists(save)).isFalse();
    }

    @Test
    void shopValidatesBeforeSpending(@TempDir Path temp) throws Exception {
        Path save = temp.resolve("save.json");
        restartWithSave(save);
        JsonNode snap = null;
        for (int seed = 23; seed < 40
                && (snap == null || !snap.get("screen").asText().equals("shop")); seed++) {
            snap = post("/api/new-run", "{\"heroClass\":\"RANGER\",\"seed\":" + seed + "}");
            int actions = 0;
            while (!snap.get("screen").asText().equals("shop")
                    && !snap.get("over").asBoolean() && actions < 400) {
                snap = stepRun(snap);
                actions++;
            }
        }
        assertThat(snap.get("screen").asText()).isEqualTo("shop");
        JsonNode shop = snap;
        for (int i = 0; i < 25; i++) {
            int gold = shop.get("run").get("gold").asInt();
            JsonNode stock = shop.get("shop").get("stock");
            int deckSize = shop.get("run").get("deckSize").asInt();
            if (stock.size() > 0 && gold >= stock.get(0).get("price").asInt()
                    && deckSize < 30 && copiesOf(shop, cardId(stock.get(0))) < 3) {
                shop = post("/api/shop-buy", "{\"kind\":\"card\",\"index\":0}");
            } else if (!shop.get("shop").get("relic").isNull()
                    && gold >= shop.get("shop").get("relic").get("price").asInt()) {
                shop = post("/api/shop-buy", "{\"kind\":\"relic\"}");
            } else if (gold >= shop.get("shop").get("healCost").asInt()) {
                shop = post("/api/shop-buy", "{\"kind\":\"heal\"}");
            } else {
                break;
            }
        }
        int goldBefore = shop.get("run").get("gold").asInt();
        int deckBefore = shop.get("run").get("deckSize").asInt();
        String attempt;
        if (shop.get("shop").get("stock").size() > 0) {
            attempt = "{\"kind\":\"card\",\"index\":0}";
        } else if (!shop.get("shop").get("relic").isNull()) {
            attempt = "{\"kind\":\"relic\"}";
        } else {
            attempt = "{\"kind\":\"heal\"}";
        }
        HttpResponse<String> denied = postRaw("/api/shop-buy", attempt);
        assertThat(denied.statusCode()).isEqualTo(400);
        JsonNode after = JSON.readTree(denied.body());
        assertThat(after.get("error").asText()).isNotEmpty();
        assertThat(after.get("run").get("gold").asInt()).isEqualTo(goldBefore);
        assertThat(after.get("run").get("deckSize").asInt()).isEqualTo(deckBefore);
    }

    @Test
    void saveResumeRoundTrip(@TempDir Path temp) throws Exception {
        Path save = temp.resolve("save.json");
        restartWithSave(save);
        assertThat(getJson("/api/state").get("hasSave").asBoolean()).isFalse();
        JsonNode snap = post("/api/new-run", "{\"heroClass\":\"KNIGHT\",\"seed\":5}");
        int actions = 0;
        while (!snap.get("over").asBoolean() && actions < 150
                && !(snap.get("screen").asText().equals("map")
                        && !snap.get("map").get("currentId").isNull())) {
            snap = stepRun(snap);
            actions++;
        }
        assertThat(snap.get("over").asBoolean()).isFalse();
        String node = snap.get("map").get("currentId").asText();
        int gold = snap.get("run").get("gold").asInt();
        int level = snap.get("run").get("level").asInt();
        JsonNode bye = post("/api/abandon", "{}");
        assertThat(bye.get("abandoned").asBoolean()).isTrue();
        assertThat(Files.exists(save)).isTrue();
        restartWithSave(save);
        assertThat(getJson("/api/state").get("hasSave").asBoolean()).isTrue();
        JsonNode back = post("/api/continue", "{}");
        assertThat(back.get("screen").asText()).isEqualTo("map");
        assertThat(back.get("map").get("currentId").asText()).isEqualTo(node);
        assertThat(back.get("run").get("gold").asInt()).isEqualTo(gold);
        assertThat(back.get("run").get("level").asInt()).isEqualTo(level);
        JsonNode next = post("/api/choose-node",
                "{\"id\":\"" + back.get("map").get("options").get(0).asText() + "\"}");
        assertThat(next.get("over").asBoolean()).isFalse();
    }

    @Test
    void continueWithoutSaveFails(@TempDir Path temp) throws Exception {
        restartWithSave(temp.resolve("save.json"));
        HttpResponse<String> missing = postRaw("/api/continue", "{}");
        assertThat(missing.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(missing.body()).get("error").asText())
                .contains("No saved campaign");
    }

    @Test
    void offCourseNodeIsRejected(@TempDir Path temp) throws Exception {
        restartWithSave(temp.resolve("save.json"));
        post("/api/new-run", "{\"heroClass\":\"KNIGHT\",\"seed\":5}");
        HttpResponse<String> bad = postRaw("/api/choose-node", "{\"id\":\"a9-L9-9\"}");
        assertThat(bad.statusCode()).isEqualTo(400);
        assertThat(JSON.readTree(bad.body()).get("error").asText()).isNotEmpty();
    }

    private void restartWithSave(Path saveFile) throws Exception {
        server.stop();
        server = new WebServer(0, new GameSession(saveFile));
        server.start();
        base = "http://127.0.0.1:" + server.port();
    }

    private JsonNode stepRun(JsonNode snap) throws Exception {
        return switch (snap.get("screen").asText()) {
            case "map" -> choosePreferred(snap);
            case "battle" -> stepBattle(snap);
            case "levelup" -> post("/api/choose-boon", "{\"index\":0}");
            case "draft" -> post("/api/choose-draft", "{\"index\":0}");
            case "shop" -> stepShop(snap);
            case "tavern" -> stepTavern(snap);
            case "event" -> stepEvent(snap);
            default -> throw new IllegalStateException("bad screen: " + snap);
        };
    }

    private JsonNode choosePreferred(JsonNode snap) throws Exception {
        Map<String, String> typeById = new HashMap<>();
        for (JsonNode node : snap.get("map").get("nodes")) {
            typeById.put(node.get("id").asText(), node.get("type").asText());
        }
        List<String> order = List.of("SHOP", "TAVERN", "EVENT", "REST", "COMBAT", "ELITE", "BOSS");
        String pick = null;
        for (String want : order) {
            for (JsonNode option : snap.get("map").get("options")) {
                if (want.equals(typeById.get(option.asText()))) {
                    pick = option.asText();
                    break;
                }
            }
            if (pick != null) {
                break;
            }
        }
        assertThat(pick).as("map offers a course").isNotNull();
        return post("/api/choose-node", "{\"id\":\"" + pick + "\"}");
    }

    private JsonNode stepShop(JsonNode snap) throws Exception {
        int gold = snap.get("run").get("gold").asInt();
        JsonNode shop = snap.get("shop");
        if (shop.get("stock").size() > 0 && gold >= shop.get("stock").get(0).get("price").asInt()
                && snap.get("run").get("deckSize").asInt() < 30
                && copiesOf(snap, cardId(shop.get("stock").get(0))) < 3) {
            return post("/api/shop-buy", "{\"kind\":\"card\",\"index\":0}");
        }
        if (!shop.get("relic").isNull() && gold >= shop.get("relic").get("price").asInt()) {
            return post("/api/shop-buy", "{\"kind\":\"relic\"}");
        }
        int hurt = snap.get("hero").get("maxHp").asInt() - snap.get("hero").get("hp").asInt();
        if (hurt > 0 && gold >= shop.get("healCost").asInt()) {
            return post("/api/shop-buy", "{\"kind\":\"heal\"}");
        }
        return post("/api/shop-leave", "{}");
    }

    private JsonNode stepTavern(JsonNode snap) throws Exception {
        int gold = snap.get("run").get("gold").asInt();
        int hurt = snap.get("hero").get("maxHp").asInt() - snap.get("hero").get("hp").asInt();
        JsonNode tavern = snap.get("tavern");
        int companions = snap.get("run").get("companions").size();
        if (hurt > 0 && gold >= tavern.get("mealCost").asInt()) {
            return post("/api/tavern", "{\"action\":\"meal\"}");
        }
        if (companions < snap.get("run").get("maxCompanions").asInt()
                && gold >= tavern.get("recruitCost").asInt()) {
            return post("/api/tavern", "{\"action\":\"recruit\"}");
        }
        if (tavern.get("canRemove").asBoolean()) {
            return post("/api/tavern", "{\"action\":\"remove\"}");
        }
        if (gold >= tavern.get("relicGold").asInt()
                && snap.get("run").get("shards").asInt() >= tavern.get("relicShards").asInt()) {
            return post("/api/tavern", "{\"action\":\"relic\"}");
        }
        return post("/api/tavern-leave", "{}");
    }

    private JsonNode stepEvent(JsonNode snap) throws Exception {
        for (JsonNode choice : snap.get("event").get("choices")) {
            if (choice.get("affordable").asBoolean()) {
                return post("/api/event-choose",
                        "{\"index\":" + choice.get("index").asInt() + "}");
            }
        }
        throw new IllegalStateException("event offers no affordable choice");
    }

    private int copiesOf(JsonNode snap, String cardId) {
        int copies = 0;
        for (JsonNode entry : snap.get("run").get("deck")) {
            if (cardId.equals(entry.get("id").asText())) {
                copies++;
            }
        }
        return copies;
    }

    private String cardId(JsonNode stockEntry) {
        return stockEntry.get("card").get("id").asText();
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
