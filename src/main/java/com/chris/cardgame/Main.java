package com.chris.cardgame;

import java.util.List;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;

public class Main {
    public static void main(String[] args) {
        System.out.println("Royan RPG Card Game - Guild Captain demo");
        CardLoader cards = CardLoader.load();
        EnemyLoader enemies = EnemyLoader.load();
        Combatant hero = Combatant.hero("Captain Royan", HeroClass.KNIGHT, 60);
        List<CardDef> deck = cards.starterDeck(HeroClass.KNIGHT);
        List<EnemyDef> foes = List.of(enemies.get("rat"), enemies.get("imp"));
        GameLoop loop = new GameLoop();
        GameLoop.BattleResult result = loop.runAutoBattle(hero, deck, foes, 42L, 50, System.out);
        System.out.println("Result: victory=" + result.victory() + ", turns=" + result.turns()
                + ", heroHp=" + result.heroHp() + ", slain=" + result.enemiesSlain()
                + "/" + result.totalEnemies());
    }
}
