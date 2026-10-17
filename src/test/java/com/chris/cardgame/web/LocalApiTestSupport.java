package com.chris.cardgame.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

abstract class LocalApiTestSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    static final HttpClient HTTP = HttpClient.newHttpClient();
    @TempDir Path temp;
    WebServer server;
    String base;

    @BeforeEach
    void start() throws Exception {
        server = new WebServer(0, new GameSession(temp.resolve("save.json")));
        server.start();
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    HttpResponse<String> state() throws Exception {
        return send(HttpRequest.newBuilder(URI.create(base + "/api/state"))
                .header("X-Royan-Client", "local").GET());
    }

    HttpRequest.Builder authorized(String path) throws Exception {
        HttpResponse<String> current = state();
        return HttpRequest.newBuilder(URI.create(base + path))
                .header("X-Royan-Client", "local")
                .header("X-Royan-Token", current.headers().firstValue("X-Royan-Token").orElse(""))
                .header("X-Royan-Revision", current.headers().firstValue("X-Royan-Revision").orElse(""));
    }

    HttpResponse<String> post(String path, String body) throws Exception {
        return send(authorized(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return HTTP.send(request.timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    JsonNode snapshot() throws Exception {
        return JSON.readTree(state().body());
    }
}
