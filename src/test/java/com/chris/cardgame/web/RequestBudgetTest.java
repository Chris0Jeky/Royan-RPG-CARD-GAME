package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Socket;
import java.net.SocketException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RequestBudgetTest extends LocalApiTestSupport {
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
