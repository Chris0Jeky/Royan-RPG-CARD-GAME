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
 * files from classpath {@code /web/} plus a small JSON game API.
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

    @FunctionalInterface
    private interface ApiCall {
        Map<String, Object> call(JsonNode body);
    }

    private final HttpServer server;
    private final ExecutorService pool;
    private final GameSession session;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, byte[]> staticFiles = Map.of(
            "/", resource("/web/index.html"),
            "/index.html", resource("/web/index.html"),
            "/app.js", resource("/web/app.js"),
            "/audio.js", resource("/web/audio.js"),
            "/style.css", resource("/web/style.css"));
    private final Map<String, byte[]> artCache = new java.util.concurrent.ConcurrentHashMap<>();

    public WebServer(int port, GameSession session) throws IOException {
        this.session = session;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/state",
                exchange -> handleApi(exchange, "GET", body -> session.snapshot()));
        server.createContext("/api/new-battle",
                exchange -> handleApi(exchange, "POST", body -> session.newBattle(
                        optText(body, "heroClass", "KNIGHT"), optSeed(body),
                        body.has("tier") ? body.get("tier").asInt() : null)));
        server.createContext("/api/new-run",
                exchange -> handleApi(exchange, "POST", body -> session.newRun(
                        optText(body, "heroClass", "KNIGHT"), optSeed(body),
                        body.has("daily") && body.get("daily").asBoolean())));
        server.createContext("/api/continue",
                exchange -> handleApi(exchange, "POST", body -> session.continueRun()));
        server.createContext("/api/play",
                exchange -> handleApi(exchange, "POST", body -> session.play(
                        optInt(body, "hand", -1), optInt(body, "target", -1))));
        server.createContext("/api/end-turn",
                exchange -> handleApi(exchange, "POST", body -> session.endTurn()));
        server.createContext("/api/choose-node",
                exchange -> handleApi(exchange, "POST",
                        body -> session.chooseNode(reqText(body, "id"))));
        server.createContext("/api/choose-boon",
                exchange -> handleApi(exchange, "POST",
                        body -> session.chooseBoon(optInt(body, "index", -1))));
        server.createContext("/api/choose-draft",
                exchange -> handleApi(exchange, "POST", body -> session.chooseDraft(
                        body.has("index") ? body.get("index").asInt() : null,
                        body.has("skip") && body.get("skip").asBoolean())));
        server.createContext("/api/shop-buy",
                exchange -> handleApi(exchange, "POST", body -> session.shopBuy(
                        reqText(body, "kind"),
                        body.has("index") ? body.get("index").asInt() : null)));
        server.createContext("/api/shop-leave",
                exchange -> handleApi(exchange, "POST", body -> session.shopLeave()));
        server.createContext("/api/tavern",
                exchange -> handleApi(exchange, "POST",
                        body -> session.tavern(reqText(body, "action"))));
        server.createContext("/api/tavern-leave",
                exchange -> handleApi(exchange, "POST", body -> session.tavernLeave()));
        server.createContext("/api/event-choose",
                exchange -> handleApi(exchange, "POST",
                        body -> session.eventChoose(optInt(body, "index", -1))));
        server.createContext("/api/abandon",
                exchange -> handleApi(exchange, "POST", body -> session.abandon()));
        server.createContext("/api/codex",
                exchange -> handleApi(exchange, "GET", body -> session.codex()));
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

    private void handleApi(HttpExchange exchange, String method, ApiCall call) throws IOException {
        if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("error", "method not allowed"));
            return;
        }
        try {
            send(exchange, 200, call.call(readBody(exchange)));
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
        String type = file != null ? contentType(path) : null;
        if (file == null && path.startsWith("/art/")) {
            String name = path.substring("/art/".length());
            if (name.matches("[A-Za-z0-9-]+\\.svg")) {
                file = artCache.computeIfAbsent(name, this::loadArt);
                type = "image/svg+xml";
            }
        }
        if (file == null) {
            send(exchange, 404, Map.of("error", "not found: " + path));
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, file.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(file);
        }
    }

    private static String optText(JsonNode body, String field, String fallback) {
        return body.has(field) ? body.get(field).asText() : fallback;
    }

    private static String reqText(JsonNode body, String field) {
        if (!body.has(field)) {
            throw new IllegalArgumentException("Missing '" + field + "'.");
        }
        return body.get(field).asText();
    }

    private static int optInt(JsonNode body, String field, int fallback) {
        return body.has(field) ? body.get(field).asInt() : fallback;
    }

    private static Long optSeed(JsonNode body) {
        return body.has("seed") && body.get("seed").isNumber()
                ? body.get("seed").asLong() : null;
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

    private byte[] loadArt(String name) {
        try (InputStream in = WebServer.class.getResourceAsStream("/web/art/" + name)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            return null;
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
