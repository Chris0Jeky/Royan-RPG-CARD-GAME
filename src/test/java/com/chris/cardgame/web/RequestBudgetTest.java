package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class RequestBudgetTest extends LocalApiTestSupport {
    @Test
    void headRefusalsDrainBeforeSendingAutomaticallyClosingHeaders() throws Exception {
        try (Socket socket = new Socket("127.0.0.1", server.port())) {
            socket.setSoTimeout(200);
            var out = socket.getOutputStream();
            out.write(("HEAD /api/state HTTP/1.1\r\nHost: 127.0.0.1:" + server.port()
                    + "\r\nContent-Length: 2\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
            var in = socket.getInputStream();
            assertThatThrownBy(in::read).isInstanceOf(SocketTimeoutException.class);
            out.write("{}".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            socket.setSoTimeout(2000);
            assertThat(new String(in.readAllBytes(), StandardCharsets.US_ASCII))
                    .startsWith("HTTP/1.1 405")
                    .containsIgnoringCase("\r\nConnection: close\r\n")
                    .containsIgnoringCase("\r\nContent-length: ")
                    .endsWith("\r\n\r\n");
        }
        assertThat(state().statusCode()).isEqualTo(200);
    }

    @Test
    void bodylessRequestsKeepTheConnectionReusable() throws Exception {
        // With drainAmount=0 the JDK server closes any exchange whose request stream was never read to
        // EOF, without telling the client; a keep-alive client then races its next request into the close.
        try (Socket socket = new Socket("127.0.0.1", server.port())) {
            socket.setSoTimeout(2000);
            var out = socket.getOutputStream();
            var in = socket.getInputStream();
            for (String path : new String[] {"/api/state", "/", "/api/state", "/art/none.svg", "/api/state"}) {
                out.write(("GET " + path + " HTTP/1.1\r\nHost: 127.0.0.1:" + server.port()
                        + "\r\nX-Royan-Client: local\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.flush();
                StringBuilder headers = new StringBuilder();
                while (!headers.toString().endsWith("\r\n\r\n")) {
                    int next = in.read();
                    assertThat(next).as("connection closed before response to " + path).isNotNegative();
                    headers.append((char) next);
                }
                var contentLength = Pattern.compile("(?i)\r\nContent-length: ([0-9]+)\r\n").matcher(headers);
                assertThat(contentLength.find()).isTrue();
                in.readNBytes(Integer.parseInt(contentLength.group(1)));
                if (path.endsWith(".svg")) {
                    // A refusal announces the close; the next loop pass is not expected to reuse it.
                    assertThat(headers.toString()).startsWith("HTTP/1.1 404")
                            .containsIgnoringCase("\r\nConnection: close\r\n");
                    return;
                }
                assertThat(headers.toString()).startsWith("HTTP/1.1 200")
                        .doesNotContainIgnoringCase("\r\nConnection: close\r\n");
                Thread.sleep(100); // let a wrongly closing server finish closing
            }
        }
    }

    @Test
    void refusalsDrainDelayedBodiesBeforeClosing() throws Exception {
        String token = state().headers().firstValue("X-Royan-Token").orElseThrow();
        var before = snapshot();
        int drainBudget = 32 * 1024 + 64 * 1024;
        for (int length : new int[] {16 * 1024, 32 * 1024 + 1, drainBudget}) {
            int status = length > 32 * 1024 ? 413 : 403;
            try (Socket socket = new Socket("127.0.0.1", server.port())) {
                socket.setSoTimeout(2000);
                var out = socket.getOutputStream();
                out.write(("POST /api/new-run HTTP/1.1\r\nHost: 127.0.0.1:" + server.port()
                        + "\r\nX-Royan-Client: local\r\nX-Royan-Token: " + (status == 413 ? token : "wrong")
                        + "\r\nContent-Type: application/json\r\nContent-Length: " + length + "\r\n\r\nx")
                        .getBytes(StandardCharsets.US_ASCII));
                out.flush();
                var in = socket.getInputStream();
                StringBuilder headers = new StringBuilder();
                while (!headers.toString().endsWith("\r\n\r\n")) {
                    int next = in.read();
                    assertThat(next).isNotNegative();
                    headers.append((char) next);
                }
                assertThat(headers.toString()).startsWith("HTTP/1.1 " + status)
                        .containsIgnoringCase("\r\nConnection: close\r\n");
                var contentLength = Pattern.compile("(?i)\r\nContent-length: ([0-9]+)\r\n").matcher(headers);
                assertThat(contentLength.find()).isTrue();
                int responseLength = Integer.parseInt(contentLength.group(1));
                assertThat(in.readNBytes(responseLength)).hasSize(responseLength);
                socket.setSoTimeout(200);
                assertThatThrownBy(in::read).isInstanceOf(SocketTimeoutException.class);
                out.write("x".repeat(length - 1).getBytes(StandardCharsets.US_ASCII));
                out.flush();
                socket.setSoTimeout(2000);
                assertThat(in.read()).isEqualTo(-1);
            }
            assertThat(snapshot()).isEqualTo(before);
        }
    }

    @Test
    void bodiesAreBoundedAndStalledClientsLeaveReadCapacity() throws Exception {
        assertThat(post("/api/new-run", "{\"padding\":\"" + "x".repeat(32768) + "\"}").statusCode()).isEqualTo(413);
        assertThat(post("/api/new-run", "{\"x\":" + "[".repeat(40) + "0" + "]".repeat(40) + "}")
                .statusCode()).isEqualTo(400);
        assertThat(post("/api/new-run", "{broken").statusCode()).isEqualTo(400);
        assertThat(send(authorized("/api/new-run").header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))).statusCode()).isEqualTo(415);
        String token = state().headers().firstValue("X-Royan-Token").orElse("");
        List<Socket> sockets = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                Socket socket = new Socket("127.0.0.1", server.port());
                sockets.add(socket);
                socket.getOutputStream().write(("POST /api/play HTTP/1.1\r\nHost: 127.0.0.1:" + server.port()
                        + "\r\nX-Royan-Client: local\r\nX-Royan-Token: " + token
                        + "\r\nContent-Type: application/json\r\nContent-Length: 1000\r\n\r\n")
                        .getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().flush();
            }
            long start = System.nanoTime();
            assertThat(state().statusCode()).isEqualTo(200);
            assertThat((System.nanoTime() - start) / 1_000_000).isLessThan(2000);
            // The server closes incomplete bodies, even if a client keeps the socket open.
            for (Socket socket : sockets) {
                socket.setSoTimeout(9000);
                try {
                    socket.getInputStream().readAllBytes();
                } catch (SocketException closed) {
                    // Reset and EOF both prove the stalled connection was closed.
                }
            }
        } finally {
            for (Socket socket : sockets) {
                socket.close();
            }
        }
        assertThat(post("/api/new-run", "{\"seed\":42}").statusCode()).isEqualTo(200);
    }
}
