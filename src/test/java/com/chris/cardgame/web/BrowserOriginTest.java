package com.chris.cardgame.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.RunState;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

/** Opt-in browser probe: -Droyan.test.browser=<Chromium/Edge executable>. */
class BrowserOriginTest extends LocalApiTestSupport {
    @Test
    void foreignBrowserPageCannotMutateSessionOrSave() throws Exception {
        String browser = System.getProperty("royan.test.browser", "");
        assumeTrue(!browser.isEmpty() && Files.isRegularFile(Path.of(browser)));
        SaveStore.save(temp.resolve("save.json"), new RunState("Captain", HeroClass.KNIGHT,
                CardLoader.load().starterDeck(HeroClass.KNIGHT), 42).toSave(null));
        post("/api/new-battle", "{\"seed\":7}");
        var before = snapshot();
        byte[] saved = Files.readAllBytes(temp.resolve("save.json"));
        String page = """
                <!doctype html><body><script>
                (async () => {
                  for (const action of ['new-run','new-battle','continue','play','end-turn',
                    'choose-node','choose-boon','choose-draft','shop-buy','shop-leave',
                    'tavern','tavern-leave','event-choose','abandon']) {
                    try {
                      await fetch('%s/api/' + action, {method:'POST', mode:'no-cors',
                        headers:{'Content-Type':'text/plain'}, body:'{"heroClass":"KNIGHT"}'});
                    } catch (_) {}
                  }
                  document.body.dataset.probe = 'complete';
                })();
                </script></body>
                """.formatted(base);
        HttpServer foreign = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        foreign.createContext("/", exchange -> {
            byte[] bytes = page.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        foreign.start();
        Process process = null;
        try {
            Path dom = temp.resolve("probe.html");
            process = new ProcessBuilder(browser, "--headless", "--disable-gpu", "--no-first-run",
                    "--user-data-dir=" + temp.resolve("browser"), "--dump-dom", "--virtual-time-budget=8000",
                    "http://127.0.0.1:" + foreign.getAddress().getPort() + "/")
                    .redirectOutput(dom.toFile()).redirectError(temp.resolve("browser.log").toFile()).start();
            assertThat(process.waitFor(25, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).as(Files.readString(temp.resolve("browser.log"))).isZero();
            assertThat(Files.readString(dom)).contains("data-probe=\"complete\"");
            assertThat(snapshot()).isEqualTo(before);
            assertThat(Files.readAllBytes(temp.resolve("save.json"))).isEqualTo(saved);
            assertThat(post("/api/end-turn", "{}").statusCode()).isEqualTo(200);
        } finally {
            foreign.stop(0);
            if (process != null) {
                var children = process.descendants().toList();
                children.forEach(ProcessHandle::destroyForcibly);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }
}
