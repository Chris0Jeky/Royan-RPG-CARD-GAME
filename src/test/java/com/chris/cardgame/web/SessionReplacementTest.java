package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

class SessionReplacementTest extends LocalApiTestSupport {
    @Test
    void rejectedReplacementsPreserveEntireSession() throws Exception {
        assertThat(post("/api/new-battle", "{\"heroClass\":\"KNIGHT\",\"seed\":7}").statusCode()).isEqualTo(200);
        post("/api/end-turn", "{}");
        assertUnchanged("/api/new-run", "{\"heroClass\":\"PIRATE\"}");
        assertUnchanged("/api/new-battle", "{\"heroClass\":\"KNIGHT\",\"tier\":999}");
        assertUnchanged("/api/continue", "{}");
        Files.writeString(temp.resolve("save.json"), "{corrupt");
        assertUnchanged("/api/continue", "{}");
        assertThat(post("/api/new-run", "{\"heroClass\":\"RANGER\",\"seed\":8}").statusCode()).isEqualTo(200);
        assertThat(snapshot().get("screen").asText()).isEqualTo("map");
        assertUnchanged("/api/new-battle", "{\"heroClass\":\"KNIGHT\",\"tier\":999}");
        assertUnchanged("/api/continue", "{}");
        assertThat(post("/api/new-battle", "{\"heroClass\":\"KNIGHT\",\"seed\":9}").statusCode()).isEqualTo(200);
        assertThat(snapshot().get("turn").asInt()).isEqualTo(1);
    }

    private void assertUnchanged(String path, String body) throws Exception {
        JsonNode before = snapshot();
        assertThat(post(path, body).statusCode()).isEqualTo(400);
        assertThat(snapshot()).isEqualTo(before);
    }
}
