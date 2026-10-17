package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class ApiOriginTest extends LocalApiTestSupport {
    @Test
    void hostMustNameTheBoundLoopbackPort() throws Exception {
        for (String host : new String[] {"hostile.invalid:" + server.port(), "127.0.0.1:1", "localhost:1"}) {
            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                socket.setSoTimeout(2000);
                socket.getOutputStream().write(("GET /api/state HTTP/1.1\r\nHost: " + host
                        + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
                assertThat(response).startsWith("HTTP/1.1 403").doesNotContain("X-Royan-Token");
            }
        }
        assertThat(state().statusCode()).isEqualTo(200);
    }

    @Test
    void foreignSimplePostsCannotChangeSessionOrCheckpoint() throws Exception {
        SaveStore.save(temp.resolve("save.json"), new RunState("Captain", HeroClass.KNIGHT,
                CardLoader.load().starterDeck(HeroClass.KNIGHT), 42).toSave(null));
        post("/api/new-battle", "{\"seed\":7}");
        JsonNode before = snapshot();
        byte[] saved = Files.readAllBytes(temp.resolve("save.json"));
        for (String action : new String[] {"new-run", "new-battle", "continue", "play", "end-turn",
                "choose-node", "choose-boon", "choose-draft", "shop-buy", "shop-leave",
                "tavern", "tavern-leave", "event-choose", "abandon"}) {
            try {
                HttpResponse<String> denied = send(HttpRequest.newBuilder(URI.create(base + "/api/" + action))
                        .header("Origin", "http://127.0.0.1:1")
                        .header("Sec-Fetch-Site", "cross-site")
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"heroClass\":\"KNIGHT\"}")));
                assertThat(denied.statusCode()).as(action).isEqualTo(403);
            } catch (IOException closed) {
                // An unread hostile request may be reset when its connection is closed.
                assertThat(closed).isNotInstanceOf(java.net.http.HttpTimeoutException.class);
            }
            assertThat(snapshot()).isEqualTo(before);
            assertThat(Files.readAllBytes(temp.resolve("save.json"))).isEqualTo(saved);
        }
        assertThat(post("/api/end-turn", "{}").statusCode()).isEqualTo(200);
    }

    @Test
    void tokenOriginFetchSiteAndRevisionAreAllEnforced() throws Exception {
        HttpRequest.Builder stale = authorized("/api/new-run");
        assertThat(post("/api/new-battle", "{\"seed\":7}").statusCode()).isEqualTo(200);
        JsonNode before = snapshot();
        assertThat(send(stale.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(409);
        assertThat(send(authorized("/api/new-run").header("Origin", "http://hostile.invalid")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(403);
        assertThat(send(authorized("/api/new-run").header("Sec-Fetch-Site", "same-site")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(403);
        assertThat(send(HttpRequest.newBuilder(URI.create(base + "/api/new-run"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(403);
        assertThat(snapshot()).isEqualTo(before);
        assertThat(send(authorized("/api/new-run").header("Origin", base)
                .header("Sec-Fetch-Site", "same-origin").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(200);
    }
}
