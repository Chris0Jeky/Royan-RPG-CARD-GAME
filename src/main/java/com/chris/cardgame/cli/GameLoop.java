package com.chris.cardgame.cli;

import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapGen;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.run.AutoBattle;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;

public class GameLoop {
    public record BattleResult(boolean victory, int turns, int heroHp, int enemiesSlain,
            int totalEnemies, List<Integer> companionHp) {
    }

    public record CampaignResult(boolean victory, int actsCleared, int level, int deckSize, int gold,
            int nodesVisited) {
    }

    public CampaignResult runAutoCampaign(long seed, HeroClass heroClass, PrintStream out) {
        CardLoader cards = CardLoader.load();
        EnemyLoader enemies = EnemyLoader.load();
        RelicLoader relics = RelicLoader.load();
        CompanionLoader companions = CompanionLoader.load();
        EventLoader events = EventLoader.load();
        RunState state = new RunState("Captain Royan", heroClass,
                cards.starterDeck(heroClass), seed);
        RunEngine runner = new RunEngine(cards, enemies, relics, companions, events);
        MapGen maps = new MapGen();
        int nodes = 0;
        for (int act = 1; act <= 3; act++) {
            state.setAct(act);
            ActMap map = maps.generate(act, seed * 31 + act);
            out.println("##### ACT " + act + " (" + map.nodes().size() + " isles) #####");
            MapNode node = map.node(map.entries().get(state.rng().nextInt(map.entries().size())));
            while (true) {
                nodes++;
                boolean survived = runner.resolve(state, node, out);
                if (!survived) {
                    out.println("### Campaign ended: defeat in Act " + act + " ###");
                    return new CampaignResult(false, act - 1, state.level(),
                            state.deck().size(), state.gold(), nodes);
                }
                if (node.type() == NodeType.BOSS) {
                    break;
                }
                node = runner.chooseNext(state, map, node);
            }
            state.hero().heal(state.hero().maxHp() * 2 / 5);
            out.println("### Act " + act + " cleared! +40% HP. ###");
        }
        out.println("### CAMPAIGN VICTORY: the Sky-Tyrant falls! ###");
        return new CampaignResult(true, 3, state.level(), state.deck().size(), state.gold(), nodes);
    }

    public BattleResult runAutoBattle(Combatant hero, List<Companion> companions,
            List<CardDef> deck, List<EnemyDef> enemies, long seed, int maxTurns, PrintStream out) {
        AutoBattle.Result auto = AutoBattle.run(hero, companions, deck, enemies, seed, maxTurns, out);
        return new BattleResult(auto.victory(), auto.turns(), auto.heroHp(), auto.enemiesSlain(),
                auto.totalEnemies(), auto.companionHp());
    }
}
