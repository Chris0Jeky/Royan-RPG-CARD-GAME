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
            if (line.equalsIgnoreCase("quit")) {
                SaveStore.save(saveFile, state.toSave(children.get(0)));
                return null;
            }
            Integer pick = parseInt(line);
            if (pick != null && pick >= 0 && pick < children.size()) {
                return map.node(children.get(pick));
            }
            out.println("Huh? Pick 0-" + (children.size() - 1) + " or 'quit'.");
        }
    }

    public boolean resolveNode(RunState state, MapNode node, Input in, PrintStream out) {
        out.println("Node " + node.id() + " [" + node.type() + "] - hero " + state.hero()
                + " | deck " + state.deck().size() + " | gold " + state.gold()
                + " | dust " + state.dust() + " | shards " + state.shards()
                + " | relics " + state.relics().size() + " | lvl " + state.level());
        return switch (node.type()) {
            case COMBAT -> battleNode(state, encounters.combat(state.act(), state.rng()),
                    false, false, in, out);
            case ELITE -> battleNode(state, encounters.elite(state.act(), state.rng()),
                    true, false, in, out);
            case BOSS -> battleNode(state, encounters.boss(state.act()), true, true, in, out);
            case REST -> {
                int heal = Math.max(1, state.hero().maxHp() * 35 / 100);
                state.hero().heal(heal);
                out.println("  Rested: +" + heal + " HP.");
                yield true;
            }
            case SHOP -> {
                manualShop(state, in, out);
                yield true;
            }
            case TAVERN -> {
                manualTavern(state, in, out);
                yield state.hero().alive();
            }
            case EVENT -> {
                EventDef event = events.pick(state);
                out.println("  Event: " + event.title() + " - " + event.text());
                for (int i = 0; i < event.choices().size(); i++) {
                    EventChoice choice = event.choices().get(i);
                    out.println("  [" + i + "] " + choice.text() + afford(choice, state));
                }
                while (true) {
                    Integer pick = parseInt(in.readLine("choice>"));
                    if (pick != null && pick >= 0 && pick < event.choices().size()
                            && affordable(event.choices().get(pick), state)) {
                        out.println("  Chose: " + event.choices().get(pick).text());
                        events.apply(state, event.choices().get(pick), out);
                        break;
                    }
                    out.println("Huh? Pick an affordable choice.");
                }
                yield state.hero().alive();
            }
        };
    }

    private String afford(EventChoice choice, RunState state) {
        if (choice.requires() == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (choice.requires().gold() > 0) {
            parts.add(choice.requires().gold() + "g");
        }
        if (choice.requires().dust() > 0) {
            parts.add(choice.requires().dust() + " dust");
        }
        if (choice.requires().shards() > 0) {
            parts.add(choice.requires().shards() + " shards");
        }
        if (choice.requires().hp() > 0) {
            parts.add("hp>" + choice.requires().hp());
        }
        String cost = parts.isEmpty() ? "" : " (needs " + String.join(", ", parts) + ")";
        return affordable(choice, state) ? cost : cost + " [CANNOT AFFORD]";
    }

    private boolean affordable(EventChoice choice, RunState state) {
        if (choice.requires() == null) {
            return true;
        }
        return state.gold() >= choice.requires().gold()
                && state.dust() >= choice.requires().dust()
                && state.shards() >= choice.requires().shards()
                && state.hero().hp() > choice.requires().hp();
    }

    public boolean battleNode(RunState state, List<EnemyDef> foes, boolean elite, boolean boss,
            Input in, PrintStream out) {
        boolean victory = manualBattle(state, foes, in, out);
        if (!victory) {
            return false;
        }
        int patchUp = state.healAfterCombat();
        if (patchUp > 0) {
            state.hero().heal(patchUp);
            out.println("  Relics mend " + patchUp + " HP.");
        }
        int base = foes.stream()
                .mapToInt(foe -> loot.rollGold(foe.goldMin(), foe.goldMax(), state.rng()))
                .sum() + (elite ? 25 : 0);
        int gold = base * (100 + state.goldPctBonus()) / 100;
        int xp = foes.stream().mapToInt(EnemyDef::xp).sum();
        state.addGold(gold);
        out.println("  Spoils: +" + gold + " gold, +" + xp + " XP.");
        if (elite) {
            int shards = boss ? 2 : 1;
            state.addShards(shards);
            out.println("  Claimed " + shards + " sky-shard(s).");
            Set<String> owned = state.relics().stream()
                    .map(RelicDef::id).collect(Collectors.toSet());
            relics.offer(owned, true, state.rng()).ifPresentOrElse(
                    relic -> {
                        state.addRelic(relic);
                        out.println("  Relic claimed: " + relic.name() + ".");
                    },
                    () -> {
                        state.addGold(50);
                        out.println("  Relic vaults empty: +50 gold instead.");
                    });
        }
        for (List<Boon> offer : state.levelUp(xp)) {
            out.println("  Level " + state.level() + "! Choose a boon:");
            for (int i = 0; i < offer.size(); i++) {
                out.println("  [" + i + "] " + offer.get(i).name() + " (" + offer.get(i).desc() + ")");
            }
            while (true) {
                Integer pick = parseInt(in.readLine("boon>"));
                if (pick != null && pick >= 0 && pick < offer.size()) {
                    state.applyBoon(offer.get(pick));
                    out.println("  Boon: " + offer.get(pick).name() + ".");
                    break;
                }
                out.println("Huh? Pick 0-" + (offer.size() - 1) + ".");
            }
        }
        int drafts = elite ? 2 : 1;
        for (int i = 0; i < drafts; i++) {
            List<CardDef> options = loot.cardOptions(state.heroClass(), state.deck(), elite, state.rng());
            if (options.isEmpty()) {
                out.println("  No draft options (collection exhausted).");
                break;
            }
            out.println("  Draft a card (or 'skip' for +" + RunEngine.SALVAGE_DUST + " dust):");
            for (int j = 0; j < options.size(); j++) {
                CardDef card = options.get(j);
                out.println("  [" + j + "] " + card.name() + " (" + card.cost() + ") "
                        + card.type() + " " + card.rarity() + " - " + describeCard(card));
            }
            while (true) {
                String line = in.readLine("draft>");
                if (line.equalsIgnoreCase("skip")) {
                    state.addDust(RunEngine.SALVAGE_DUST);
                    out.println("  Salvaged: +" + RunEngine.SALVAGE_DUST + " dust.");
                    break;
                }
                Integer pick = parseInt(line);
                if (pick != null && pick >= 0 && pick < options.size()) {
                    if (state.addCard(options.get(pick))) {
                        out.println("  Drafted: " + options.get(pick).name() + ".");
                    } else {
                        out.println("  Deck is full.");
                    }
                    break;
                }
                out.println("Huh? Pick 0-" + (options.size() - 1) + " or 'skip'.");
            }
        }
        return true;
    }

    public boolean manualBattle(RunState state, List<EnemyDef> foes, Input in, PrintStream out) {
        List<Combatant> fighters = state.companions().stream()
                .map(Companion::toCombatant).toList();
        List<CompanionRole> roles = state.companions().stream()
                .map(companion -> companion.def().role()).toList();
        List<Integer> powers = state.companions().stream()
                .map(companion -> companion.def().power()).toList();
        CombatState battle = engine.newBattle(state.hero(), fighters, roles, powers,
                state.deck(), foes, state.rng().nextLong());
        out.println("=== Battle: " + state.hero().name() + " vs " + foes.size() + " foes ===");
        out.println("Commands: 'play <card> [foe]', 'end'. Aoe cards need no foe.");
