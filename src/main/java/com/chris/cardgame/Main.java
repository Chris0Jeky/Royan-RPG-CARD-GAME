package com.chris.cardgame;

import java.nio.file.Path;
import java.util.Random;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.cli.Input;
import com.chris.cardgame.cli.InteractiveLoop;
import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.cli.ScannerInput;
import com.chris.cardgame.model.HeroClass;

public class Main {
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
            default -> System.out.println("Usage: play [CLASS] [seed] | continue | auto [seed] [CLASS]");
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

    private static void printResult(InteractiveLoop.Result result) {
        System.out.println("Result: victory=" + result.victory()
                + ", acts=" + result.actsCleared() + "/3"
                + ", level=" + result.level()
                + (result.abandoned() ? ", abandoned (progress saved)" : ""));
    }
}
