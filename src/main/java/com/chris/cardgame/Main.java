package com.chris.cardgame;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Path;
import java.util.Random;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.cli.Input;
import com.chris.cardgame.cli.InteractiveLoop;
import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.cli.ScannerInput;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.web.GameSession;
import com.chris.cardgame.web.WebServer;

public class Main {
    /** How many successor ports to try when the requested serve port is busy. */
    static final int PORT_FALLBACK_TRIES = 20;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase() : "play";
        try {
            Long.parseLong(args.length > 0 ? args[0] : "");
            mode = "auto-legacy";
        } catch (NumberFormatException e) {
            // not a bare seed; use mode as given
        }
        switch (mode) {
            case "auto" -> {
                long seed = args.length > 1 ? Long.parseLong(args[1]) : 42L;
                HeroClass heroClass = args.length > 2
                        ? HeroClass.valueOf(args[2].toUpperCase()) : HeroClass.KNIGHT;
                runAuto(seed, heroClass);
            }
            case "auto-legacy" -> {
                long seed = Long.parseLong(args[0]);
                HeroClass heroClass = args.length > 1
                        ? HeroClass.valueOf(args[1].toUpperCase()) : HeroClass.KNIGHT;
                runAuto(seed, heroClass);
            }
            case "continue" -> {
                Path save = SaveStore.defaultPath();
                if (!java.nio.file.Files.exists(save)) {
                    System.out.println("No saved campaign at " + save + ".");
                    break;
                }
                Input in = new ScannerInput(System.in, System.out);
                InteractiveLoop.Result result =
                        new InteractiveLoop().continueCampaign(in, System.out, save);
                printResult(result);
            }
            case "play" -> {
                Input in = new ScannerInput(System.in, System.out);
                HeroClass heroClass = args.length > 1
                        ? HeroClass.valueOf(args[1].toUpperCase()) : promptClass(in);
                long seed = args.length > 2 ? Long.parseLong(args[2]) : new Random().nextLong();
                InteractiveLoop.Result result = new InteractiveLoop().runCampaign(
                        heroClass, seed, in, System.out, SaveStore.defaultPath());
                printResult(result);
            }
            case "serve" -> {
                int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
                runServe(port);
            }
            default -> System.out.println(
                    "Usage: play [CLASS] [seed] | continue | auto [seed] [CLASS] | serve [port]");
        }
    }

    /**
     * Binds the web UI, falling back to successor ports when the requested
     * port is busy. Port 0 binds any free port (no fallback needed).
     */
    static WebServer bindWithFallback(int requestedPort, GameSession session) throws IOException {
        if (requestedPort == 0) {
            return new WebServer(0, session);
        }
        BindException lastBusy = null;
        for (int port = requestedPort; port < requestedPort + PORT_FALLBACK_TRIES; port++) {
            try {
                return new WebServer(port, session);
            } catch (BindException busy) {
                lastBusy = busy;
            }
        }
        throw lastBusy;
    }

    /**
     * Opens the game URL in the default browser. Headless-safe: returns false
     * (and never throws) when no browser integration is available.
     */
    static boolean openBrowser(String url) {
        try {
            if (!java.awt.Desktop.isDesktopSupported()) {
                return false;
            }
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                return false;
            }
            desktop.browse(new java.net.URI(url));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static HeroClass promptClass(Input in) {
        while (true) {
            String line = in.readLine("Choose your captain [knight/ranger/runemage]:");
            try {
                HeroClass heroClass = HeroClass.valueOf(line.trim().toUpperCase());
                if (heroClass != HeroClass.NEUTRAL) {
                    return heroClass;
                }
            } catch (IllegalArgumentException e) {
                // fall through
            }
            System.out.println("Huh? knight, ranger, or runemage.");
        }
    }

    private static void runAuto(long seed, HeroClass heroClass) {
        System.out.println("Royan RPG Card Game - Guild Captain campaign demo");
        GameLoop loop = new GameLoop();
        GameLoop.CampaignResult result = loop.runAutoCampaign(seed, heroClass, System.out);
        System.out.println("Result: victory=" + result.victory()
                + ", hero=" + heroClass
                + ", acts=" + result.actsCleared() + "/3"
                + ", level=" + result.level()
                + ", deck=" + result.deckSize()
                + ", gold=" + result.gold()
                + ", nodes=" + result.nodesVisited());
    }

    private static void runServe(int port) {
        try {
            WebServer server = bindWithFallback(port, new GameSession());
            server.start();
            String url = "http://localhost:" + server.port() + "/";
            if (port != 0 && server.port() != port) {
                System.out.println("Port " + port + " was busy; using port "
                        + server.port() + " instead.");
            }
            System.out.println("Royan web UI at " + url);
            if (openBrowser(url)) {
                System.out.println("Opened in your default browser. Ctrl+C to stop.");
            } else {
                System.out.println("Open that address in a browser. Ctrl+C to stop.");
            }
            new java.util.concurrent.CountDownLatch(1).await();
        } catch (Exception e) {
            System.out.println("Could not start web server: " + e.getMessage());
        }
    }

    private static void printResult(InteractiveLoop.Result result) {
        System.out.println("Result: victory=" + result.victory()
                + ", acts=" + result.actsCleared() + "/3"
                + ", level=" + result.level()
                + (result.abandoned() ? ", abandoned (progress saved)" : ""));
    }
}
