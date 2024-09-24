package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.map.ActMap;
import com.chris.cardgame.map.MapNode;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.EnemyDef;

public class RunEngine {
    private final EncounterGen encounters;
    private final LootGen loot;
    private final Events events;
    private final Shop shop;

    public RunEngine(CardLoader cards, EnemyLoader enemies) {
        this.encounters = new EncounterGen(enemies);
        this.loot = new LootGen(cards);
        this.events = new Events(cards);
        this.shop = new Shop(cards);
    }

    public boolean resolve(RunState state, MapNode node, PrintStream out) {
        out.println("Node " + node.id() + " [" + node.type() + "] - hero " + state.hero()
                + " | deck " + state.deck().size() + " | gold " + state.gold()
                + " | lvl " + state.level());
        return switch (node.type()) {
            case COMBAT -> combat(state, encounters.combat(state.act(), state.rng()), false, out);
            case ELITE -> combat(state, encounters.elite(state.act(), state.rng()), true, out);
            case BOSS -> combat(state, encounters.boss(state.act()), true, out);
            case REST -> {
                int heal = Math.max(1, state.hero().maxHp() * 35 / 100);
                state.hero().heal(heal);
                out.println("  Rested: +" + heal + " HP.");
                yield true;
            }
            case SHOP -> {
                shop.visit(state, out);
                yield true;
            }
            case EVENT -> {
                events.resolve(state, out);
                yield state.hero().alive();
            }
        };
    }

    public MapNode chooseNext(RunState state, ActMap map, MapNode node) {
        return node.children().stream()
