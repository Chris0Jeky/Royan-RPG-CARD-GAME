package com.chris.cardgame.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded HTTP server for the browser UI. Serves dependency-free static
 * files from classpath {@code /web/} plus a small JSON battle API.
 * Zero new dependencies: JDK HttpServer + the Jackson already on board.
 */
public class WebServer {
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            ".html", "text/html; charset=utf-8",
            ".css", "text/css; charset=utf-8",
            ".js", "text/javascript; charset=utf-8",
            ".json", "application/json; charset=utf-8",
            ".svg", "image/svg+xml",
            ".png", "image/png");

    private final HttpServer server;
    private final ExecutorService pool;
    private final GameSession session;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, byte[]> staticFiles = Map.of(
            "/", resource("/web/index.html"),
            "/index.html", resource("/web/index.html"),
            "/app.js", resource("/web/app.js"),
            "/style.css", resource("/web/style.css"));

    public WebServer(int port, GameSession session) throws IOException {
        this.session = session;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/new-battle", this::handleNewBattle);
        server.createContext("/api/play", this::handlePlay);
        server.createContext("/api/end-turn", this::handleEndTurn);
        server.createContext("/", this::handleStatic);
        pool = Executors.newFixedThreadPool(4, daemonFactory());
        server.setExecutor(pool);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
        pool.shutdownNow();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleState(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        send(exchange, 200, session.snapshot());
    }

    private void handleNewBattle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        try {
            JsonNode body = readBody(exchange);
            String heroClass = body.has("heroClass") ? body.get("heroClass").asText() : "KNIGHT";
            Long seed = body.has("seed") && body.get("seed").isNumber()
                    ? body.get("seed").asLong() : null;
            send(exchange, 200, session.newBattle(heroClass, seed));
        } catch (IllegalArgumentException e) {
            send(exchange, 400, error(e));
        }
    }

    private void handlePlay(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        try {
            JsonNode body = readBody(exchange);
            int hand = body.has("hand") ? body.get("hand").asInt() : -1;
            int target = body.has("target") ? body.get("target").asInt() : -1;
            send(exchange, 200, session.play(hand, target));
        } catch (IllegalArgumentException | IllegalStateException e) {
            send(exchange, 400, error(e));
        }
    }

    private void handleEndTurn(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        try {
            readBody(exchange);
            send(exchange, 200, session.endTurn());
        } catch (IllegalArgumentException | IllegalStateException e) {
            send(exchange, 400, error(e));
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        byte[] file = staticFiles.get(path);
        if (file == null) {
            send(exchange, 404, Map.of("error", "not found: " + path));
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", contentType(path));
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, file.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(file);
        }
    }

    private Map<String, Object> error(RuntimeException e) {
        Map<String, Object> response = new LinkedHashMap<>(session.snapshot());
        response.put("error", e.getMessage());
        return response;
    }

    private JsonNode readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            byte[] bytes = in.readAllBytes();
            if (bytes.length == 0) {
                return json.createObjectNode();
            }
            return json.readTree(bytes);
        }
    }

    private void send(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentType(String path) {
        if (path.endsWith("/")) {
            return CONTENT_TYPES.get(".html");
        }
        int dot = path.lastIndexOf('.');
        String ext = dot >= 0 ? path.substring(dot) : "";
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }

    private static byte[] resource(String path) {
        try (InputStream in = WebServer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("missing web resource: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read web resource: " + path, e);
        }
    }

    private static ThreadFactory daemonFactory() {
        AtomicInteger next = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, "royan-web-" + next.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

}
