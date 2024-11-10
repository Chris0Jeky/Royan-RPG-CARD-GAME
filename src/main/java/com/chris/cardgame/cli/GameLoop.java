package com.chris.cardgame.cli;

import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapGen;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.map.NodeType;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.Row;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;

public class GameLoop {
    private final CombatEngine engine = new CombatEngine();

    public record BattleResult(boolean victory, int turns, int heroHp, int enemiesSlain, int totalEnemies) {
    }

    public record CampaignResult(boolean victory, int actsCleared, int level, int deckSize, int gold,
            int nodesVisited) {
    }

    public CampaignResult runAutoCampaign(long seed, HeroClass heroClass, PrintStream out) {
        CardLoader cards = CardLoader.load();
        EnemyLoader enemies = EnemyLoader.load();
        RelicLoader relics = RelicLoader.load();
        RunState state = new RunState("Captain Royan", heroClass,
                cards.starterDeck(heroClass), seed);
        RunEngine runner = new RunEngine(cards, enemies, relics);
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

    public BattleResult runAutoBattle(Combatant hero, List<CardDef> deck, List<EnemyDef> enemies,
            long seed, int maxTurns, PrintStream out) {
        CombatState state = engine.newBattle(hero, List.of(), deck, enemies, seed);
        out.println("=== Battle: " + hero.name() + " vs " + enemies.size() + " foes (seed " + seed + ") ===");
        while (!state.over() && state.turn() <= maxTurns) {
            describe(state, out);
            autoTurn(state, out);
            if (!state.over()) {
                engine.endTurn(state);
            }
        }
        int slain = (int) state.enemies().stream().filter(e -> !e.alive()).count();
        BattleResult result = new BattleResult(state.victory(), state.turn(), state.hero().hp(),
                slain, state.enemies().size());
        out.println(result.victory() ? ">>> VICTORY in " + result.turns() + " turns"
                : ">>> DEFEAT after " + result.turns() + " turns");
        return result;
    }

    private void autoTurn(CombatState state, PrintStream out) {
        while (true) {
            int pick = pickCard(state);
            if (pick < 0) {
                return;
            }
            CardDef card = state.hand().get(pick);
            int target = firstAliveTarget(state);
            engine.playCard(state, pick, target);
            out.println("  plays " + card.name() + " (" + card.cost() + " energy)");
            if (state.over()) {
                return;
            }
        }
    }

    private int pickCard(CombatState state) {
        List<CardDef> hand = state.hand();
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < hand.size(); i++) {
            CardDef card = hand.get(i);
            if (card.unplayable() || card.cost() > state.energy()) {
                continue;
            }
            int score = score(card, state);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private int score(CardDef card, CombatState state) {
        boolean hurt = state.hero().hp() <= state.hero().maxHp() * 3 / 5;
        long foes = state.enemies().stream().filter(Combatant::alive).count();
        int targets = card.aoe() ? (int) Math.max(1, foes) : 1;
        int damage = card.damage() * card.hits() * targets;
        return switch (card.type()) {
            case GUARD -> hurt ? 100 + card.block() : 20 + card.block();
            case STRIKE -> 60 + damage;
            case TRICK -> 50 + damage + card.draw() * 5 + (card.weak() + card.vulnerable()) * 4 * targets
                    + card.heal() * (hurt ? 8 : 2) + card.energy() * 15;
            case POWER -> 40 + card.strength() * 10 + card.energy() * 15 + card.draw() * 5;
            case CURSE -> Integer.MIN_VALUE;
        };
    }

    private int firstAliveTarget(CombatState state) {
        List<Combatant> enemies = state.enemies();
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive() && enemies.get(i).row() == Row.FRONT) {
                return i;
            }
        }
        for (int i = 0; i < enemies.size(); i++) {
            if (enemies.get(i).alive()) {
                return i;
            }
        }
        return 0;
    }

    private void describe(CombatState state, PrintStream out) {
        out.println("-- turn " + state.turn() + " | energy " + state.energy()
                + " | hero " + state.hero() + " --");
        for (int i = 0; i < state.enemies().size(); i++) {
            Combatant enemy = state.enemies().get(i);
            String intent = enemy.alive() ? " [" + state.intents().get(i) + "]" : " [DOWN]";
            out.println("  foe " + i + ": " + enemy + intent);
        }
        StringBuilder hand = new StringBuilder("  hand:");
        for (int i = 0; i < state.hand().size(); i++) {
            CardDef card = state.hand().get(i);
            hand.append(" [").append(i).append("] ").append(card.name())
                    .append("(").append(card.cost()).append(")");
        }
        out.println(hand);
    }
}
