package com.chris.cardgame;

import com.chris.cardgame.cli.GameLoop;

public class Main {
    public static void main(String[] args) {
        System.out.println("Royan RPG Card Game - Guild Captain campaign demo");
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        GameLoop loop = new GameLoop();
        GameLoop.CampaignResult result = loop.runAutoCampaign(seed, System.out);
        System.out.println("Result: victory=" + result.victory()
                + ", acts=" + result.actsCleared() + "/3"
                + ", level=" + result.level()
                + ", deck=" + result.deckSize()
                + ", gold=" + result.gold()
                + ", nodes=" + result.nodesVisited());
    }
}
