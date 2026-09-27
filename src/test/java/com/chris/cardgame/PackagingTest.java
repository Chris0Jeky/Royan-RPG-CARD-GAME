package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.chris.cardgame.web.GameSession;
import com.chris.cardgame.web.WebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class PackagingTest {
    private final List<WebServer> servers = new ArrayList<>();

    @AfterEach
    void stopServers() {
        for (WebServer server : servers) {
            server.stop();
        }
        servers.clear();
    }

    @Test
    void freePortIsUsedAsRequested() throws Exception {
        int free = findFreePort();
        WebServer server = Main.bindWithFallback(free, new GameSession());
        servers.add(server);
        assertThat(server.port()).isEqualTo(free);
    }

    @Test
    void busyPortFallsBackToSuccessor() throws Exception {
        WebServer busy = new WebServer(0, new GameSession());
        servers.add(busy);
        int taken = busy.port();
        WebServer server = Main.bindWithFallback(taken, new GameSession());
        servers.add(server);
        assertThat(server.port()).isGreaterThan(taken);
        assertThat(server.port()).isLessThan(taken + Main.PORT_FALLBACK_TRIES);
    }

    @Test
    void portZeroBindsAnyFreePort() throws Exception {
        WebServer server = Main.bindWithFallback(0, new GameSession());
        servers.add(server);
        assertThat(server.port()).isGreaterThan(0);
    }

    @Test
    void openBrowserIsHeadlessSafe() {
        Assumptions.assumeTrue(java.awt.GraphicsEnvironment.isHeadless(),
                "skipped on desktops: must not pop a browser during tests");
        assertThat(Main.openBrowser("http://localhost:9/")).isFalse();
    }

    @Test
    void shadePluginDeclaresMainClass() throws Exception {
        Path pom = Paths.get("pom.xml");
        String xml = Files.readString(pom, StandardCharsets.UTF_8);
        assertThat(xml).contains("maven-shade-plugin");
        assertThat(xml).contains("com.chris.cardgame.Main");
    }

    @Test
    void packagedJarManifestDeclaresMainClass() throws Exception {
        Path jar = findFatJar();
        Assumptions.assumeTrue(jar != null,
                "run mvn package first: no target/royan-*.jar yet");
        assertThat(jar).as("fat jar from mvn package").isNotNull();
        try (JarFile archive = new JarFile(jar.toFile())) {
            Manifest manifest = archive.getManifest();
            assertThat(manifest).isNotNull();
            Attributes attrs = manifest.getMainAttributes();
            assertThat(attrs.getValue(Attributes.Name.MAIN_CLASS))
                    .isEqualTo("com.chris.cardgame.Main");
            assertThat(archive.getEntry(
                    "com/fasterxml/jackson/databind/ObjectMapper.class"))
                    .as("fat jar bundles Jackson").isNotNull();
            assertThat(archive.getEntry("web/index.html"))
                    .as("fat jar bundles web UI").isNotNull();
            assertThat(archive.getEntry("data/cards.json"))
                    .as("fat jar bundles game data").isNotNull();
        }
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket probe = new ServerSocket(0)) {
            probe.setReuseAddress(true);
            return probe.getLocalPort();
        }
    }

    private static Path findFatJar() throws Exception {
        Path target = Paths.get("target");
        if (!Files.isDirectory(target)) {
            return null;
        }
        try (var jars = Files.list(target)) {
            return jars.filter(p -> p.getFileName().toString().startsWith("royan-"))
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().endsWith("-sources.jar"))
                    .filter(p -> !p.getFileName().toString().endsWith("-shaded.jar"))
                    .sorted()
                    .findFirst()
                    .orElse(null);
        }
    }
}
