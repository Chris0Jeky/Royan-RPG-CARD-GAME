package com.chris.cardgame.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Embedded HTTP server for the browser UI. Serves dependency-free static
 * files from classpath {@code /web/} plus a small JSON game API.
 * Zero new dependencies: JDK HttpServer + the Jackson already on board.
 */
public class WebServer {
    static {
        // JDK HttpServer snapshots these limits when its first server is created.
        System.setProperty("sun.net.httpserver.maxReqTime", "5");
        System.setProperty("sun.net.httpserver.maxRspTime", "5");
        System.setProperty("sun.net.httpserver.timerMillis", "1000");
        System.setProperty("sun.net.httpserver.maxReqHeaders", "32");
        System.setProperty("jdk.httpserver.maxConnections", "32");
        System.setProperty("sun.net.httpserver.drainAmount", "0");
    }

    private static final int MAX_BODY = 32 * 1024;
    /** Exchange attribute set once the request body was read to EOF, so the connection can be reused. */
    private static final String BODY_DONE = "royan.bodyDone";
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
    private final ObjectMapper json = new ObjectMapper(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final Semaphore bodyReaders = new Semaphore(2);
    private final ExecutorService bodyDrainer = Executors.newSingleThreadExecutor(daemonFactory());
    private final String token = UUID.randomUUID().toString();
    private long revision;
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
        bodyDrainer.shutdownNow();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleApi(HttpExchange exchange, String method, ApiCall call) throws IOException {
        boolean reading = false;
        try {
            if (!validHost(exchange)) {
                send(exchange, 403, Map.of("error", "untrusted host"));
                return;
            }
            if (!exchange.getHttpContext().getPath().equals(exchange.getRequestURI().getPath())) {
                send(exchange, 404, Map.of("error", "not found"));
                return;
            }
            if (!method.equalsIgnoreCase(exchange.getRequestMethod())) {
                send(exchange, 405, Map.of("error", "method not allowed"));
                return;
            }
            boolean trusted = trustedClient(exchange);
            if (method.equals("POST")) {
                if (!trusted || !token.equals(singleHeader(exchange, "X-Royan-Token"))) {
                    send(exchange, 403, Map.of("error", "untrusted request"));
                    return;
                }
                String type = singleHeader(exchange, "Content-Type");
                if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
                    rejectUnread(exchange, 415);
                    return;
                }
                reading = bodyReaders.tryAcquire();
                if (!reading) {
                    rejectUnread(exchange, 503);
                    return;
                }
            }
            JsonNode body = method.equals("POST") ? readBody(exchange) : emptyBody(exchange);
            synchronized (session) {
                if (trusted) {
                    exchange.getResponseHeaders().set("X-Royan-Token", token);
                    exchange.getResponseHeaders().set("X-Royan-Revision", Long.toString(revision));
                }
                String path = exchange.getRequestURI().getPath();
                if ((path.equals("/api/new-run") || path.equals("/api/new-battle")
                        || path.equals("/api/continue") || path.equals("/api/abandon"))
                        && !Long.toString(revision).equals(singleHeader(exchange, "X-Royan-Revision"))) {
                    send(exchange, 409, error(new IllegalStateException(
                            "The voyage changed. Review it before replacing it.")));
                    return;
                }
                Map<String, Object> result = call.call(body);
                if (method.equals("POST")) {
                    revision++;
                    exchange.getResponseHeaders().set("X-Royan-Revision", Long.toString(revision));
                }
                send(exchange, 200, result);
            }
        } catch (RequestRejected e) {
            rejectUnread(exchange, e.status);
        } catch (JsonProcessingException e) {
            send(exchange, 400, Map.of("error", "invalid JSON body"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            send(exchange, 400, error(e));
        } catch (IOException | RuntimeException e) {
            exchange.close();
            throw e;
        } finally {
            if (reading) {
                bodyReaders.release();
            }
        }
    }

    private boolean validHost(HttpExchange exchange) {
        String host = singleHeader(exchange, "Host");
        return ("127.0.0.1:" + port()).equals(host) || ("localhost:" + port()).equals(host);
    }

    private boolean trustedClient(HttpExchange exchange) {
        String origin = singleHeader(exchange, "Origin");
        String site = singleHeader(exchange, "Sec-Fetch-Site");
        if (exchange.getRequestHeaders().containsKey("Origin")
                && !("http://" + singleHeader(exchange, "Host")).equals(origin)) {
            return false;
        }
        if (exchange.getRequestHeaders().containsKey("Sec-Fetch-Site") && !"same-origin".equals(site)) {
            return false;
        }
        return origin != null || "same-origin".equals(site)
                || "local".equals(singleHeader(exchange, "X-Royan-Client"));
    }

    private static String singleHeader(HttpExchange exchange, String name) {
        var values = exchange.getRequestHeaders().get(name);
        return values != null && values.size() == 1 ? values.get(0) : null;
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!validHost(exchange)) {
            send(exchange, 403, Map.of("error", "untrusted host"));
            return;
        }
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
        emptyBody(exchange);
        if (exchange.getAttribute(BODY_DONE) == null) {
            exchange.getResponseHeaders().set("Connection", "close");
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
        String length = singleHeader(exchange, "Content-Length");
        if (length == null || !length.matches("[0-9]+")
                || exchange.getRequestHeaders().containsKey("Transfer-Encoding")) {
            throw new RequestRejected(411, "one Content-Length required; chunked bodies unsupported");
        }
        if (length.length() > 5 || Long.parseLong(length) > MAX_BODY) {
            throw new RequestRejected(413, "request body exceeds 32 KiB");
        }
        try (InputStream in = exchange.getRequestBody()) {
            int expected = Integer.parseInt(length);
            byte[] bytes = in.readNBytes(expected + 1);
            if (bytes.length != expected) {
                throw new RequestRejected(400, "incomplete request body");
            }
            exchange.setAttribute(BODY_DONE, Boolean.TRUE);
            if (bytes.length == 0) {
                return json.createObjectNode();
            }
            JsonNode body = json.readTree(bytes);
            if (body == null || !body.isObject()) {
                throw new RequestRejected(400, "JSON object required");
            }
            return body;
        }
    }

    /**
     * Reads a body-less request to EOF. With drainAmount=0 the JDK server closes, without telling the
     * client, any exchange whose request stream was never read to EOF, so a keep-alive client races
     * its next request into that close. A request that does declare a body is left unread and the
     * response says {@code Connection: close} instead.
     */
    private JsonNode emptyBody(HttpExchange exchange) throws IOException {
        String length = singleHeader(exchange, "Content-Length");
        if (!exchange.getRequestHeaders().containsKey("Transfer-Encoding")
                && (length == null ? !exchange.getRequestHeaders().containsKey("Content-Length")
                        : length.equals("0"))) {
            exchange.getRequestBody().read();
            exchange.setAttribute(BODY_DONE, Boolean.TRUE);
        }
        return json.createObjectNode();
    }

    private static class RequestRejected extends IOException {
        final int status;

        RequestRejected(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private void rejectUnread(HttpExchange exchange, int status) throws IOException {
        send(exchange, status, Map.of());
    }

    private void send(HttpExchange exchange, int status, Map<String, ?> body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if (status >= 400 || exchange.getAttribute(BODY_DONE) == null) {
            exchange.getResponseHeaders().set("Connection", "close");
        }
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            bodyDrainer.execute(() -> {
                try {
                    // JDK HEAD headers close the exchange, so drain before sending them.
                    drainRequest(exchange);
                    exchange.sendResponseHeaders(status, -1);
                } catch (IOException ignored) {
                    // The client or the existing deadline may already have closed the connection.
                } finally {
                    exchange.close();
                }
            });
            return;
        }
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            OutputStream out = exchange.getResponseBody();
            out.write(bytes);
            out.flush();
        } finally {
            if (status >= 400) {
                // Keep request workers free while bounded drains wait on the JDK read deadline.
                bodyDrainer.execute(() -> {
                    try {
                        drainRequest(exchange);
                    } finally {
                        exchange.close();
                    }
                });
            } else {
                exchange.close();
            }
        }
    }

    private void drainRequest(HttpExchange exchange) {
        try {
            exchange.getRequestBody().readNBytes(MAX_BODY + 64 * 1024);
        } catch (IOException ignored) {
            // The body was already closed, the client left, or the read deadline expired.
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
