package com.chris.cardgame.cli;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapGen;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.CompanionRole;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.EventDef;
import com.chris.cardgame.model.EventDef.EventChoice;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.RelicDef;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.Events;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.SaveData;
import com.chris.cardgame.run.Shop;
import com.chris.cardgame.run.Tavern;

public class InteractiveLoop {
    public record Result(boolean victory, int actsCleared, int level, boolean abandoned) {
    }

    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();
    private final RelicLoader relics = RelicLoader.load();
    private final CompanionLoader companions = CompanionLoader.load();
    private final EventLoader eventDefs = EventLoader.load();
    private final EncounterGen encounters = new EncounterGen(enemies);
    private final LootGen loot = new LootGen(cards);
    private final Events events = new Events(eventDefs, cards, relics, companions);

    public Result runCampaign(HeroClass heroClass, long seed, Input in, PrintStream out,
            Path saveFile) {
        RunState state = new RunState("Captain Royan", heroClass,
                cards.starterDeck(heroClass), seed);
        out.println("Royan RPG Card Game - " + heroClass + " campaign (seed " + seed + ")");
        return runFromState(state, null, in, out, saveFile);
    }

    public Result continueCampaign(Input in, PrintStream out, Path saveFile) {
        SaveData save = SaveStore.load(saveFile);
        RunState state = RunState.fromSave(save, cards, relics, companions);
        out.println("Royan RPG Card Game - resuming " + save.heroClass() + ", Act " + save.act());
        return runFromState(state, save.nodeId(), in, out, saveFile);
    }

    private Result runFromState(RunState state, String startNode, Input in, PrintStream out,
            Path saveFile) {
        MapGen maps = new MapGen();
        for (int act = state.act(); act <= 3; act++) {
            if (act != state.act()) {
                state.setAct(act);
            }
            ActMap map = maps.generate(act, state.seed() * 31 + act);
            out.println("##### ACT " + act + " (" + map.nodes().size() + " isles) #####");
            MapNode node;
            if (startNode != null) {
                node = map.node(startNode);
            } else {
                node = promptEntry(state, map, in, out, saveFile, act);
                if (node == null) {
                    return new Result(false, act - 1, state.level(), true);
                }
            }
            startNode = null;
            while (true) {
                boolean survived = resolveNode(state, node, in, out);
                if (!survived) {
                    out.println("### The Captain falls in Act " + act + " ###");
                    SaveStore.delete(saveFile);
                    return new Result(false, act - 1, state.level(), false);
                }
                if (node.type() == com.chris.cardgame.map.NodeType.BOSS) {
                    break;
                }
                node = promptNext(state, map, node, in, out, saveFile, act);
                if (node == null) {
                    return new Result(false, act - 1, state.level(), true);
                }
                SaveStore.save(saveFile, state.toSave(node.id()));
            }
            state.hero().heal(state.hero().maxHp() * 2 / 5);
            out.println("### Act " + act + " cleared! +40% HP. ###");
            if (act < 3) {
                state.setAct(act + 1);
                SaveStore.save(saveFile, state.toSave(null));
                state.setAct(act);
            }
        }
        out.println("### CAMPAIGN VICTORY: the Sky-Tyrant falls! ###");
        SaveStore.delete(saveFile);
        return new Result(true, 3, state.level(), false);
    }

    private MapNode promptEntry(RunState state, ActMap map, Input in, PrintStream out,
            Path saveFile, int act) {
        out.println("Choose your landing isle (or 'quit'):");
        for (int i = 0; i < map.entries().size(); i++) {
            MapNode entry = map.node(map.entries().get(i));
            out.println("  [" + i + "] " + entry.id() + " " + entry.type()
                    + " -> " + entry.children().size() + " paths");
        }
        while (true) {
            String line = in.readLine("isle>");
            if (line.equalsIgnoreCase("quit")) {
                SaveStore.save(saveFile, state.toSave(null));
                return null;
            }
            Integer pick = parseInt(line);
            if (pick != null && pick >= 0 && pick < map.entries().size()) {
                return map.node(map.entries().get(pick));
            }
            out.println("Huh? Pick 0-" + (map.entries().size() - 1) + " or 'quit'.");
        }
    }

    private MapNode promptNext(RunState state, ActMap map, MapNode node, Input in, PrintStream out,
            Path saveFile, int act) {
        out.println("Choose your course (or 'quit'):");
        List<String> children = node.children();
        for (int i = 0; i < children.size(); i++) {
            MapNode child = map.node(children.get(i));
            out.println("  [" + i + "] " + child.id() + " " + child.type());
        }
        while (true) {
            String line = in.readLine("course>");
