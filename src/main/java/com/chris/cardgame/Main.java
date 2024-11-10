package com.chris.cardgame;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.model.HeroClass;

public class Main {
    public static void main(String[] args) {
        System.out.println("Royan RPG Card Game - Guild Captain campaign demo");
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42L;
        HeroClass heroClass = args.length > 1 ? HeroClass.valueOf(args[1].toUpperCase()) : HeroClass.KNIGHT;
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
}
