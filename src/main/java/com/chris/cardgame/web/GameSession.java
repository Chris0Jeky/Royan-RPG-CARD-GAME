package com.chris.cardgame.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SplittableRandom;

import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.combat.Intent;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;

/**
 * One browser session's battle state. W1 scope: a single Act 1 skirmish
 * (hero + starter deck vs a generated encounter). All methods are
 * synchronized: the embedded server handles requests on a small pool.
 */
public class GameSession {
    public static final String HERO_NAME = "Captain Royan";
    private static final int LOG_CAP = 300;
    private static final int LOG_TAIL = 80;

    private static final Map<HeroClass, String> BLURBS = Map.of(
            HeroClass.KNIGHT, "Block, heavy hits, honest work. Easy to learn.",
            HeroClass.RANGER, "Cheap multi-hit strikes, card draw, debuffs. Strong and fast.",
            HeroClass.RUNEMAGE, "Burst damage, energy tricks, big powers. Fragile, explosive.");

    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EncounterGen encounters = new EncounterGen(EnemyLoader.load());

    private HeroClass heroClass;
    private long seed;
    private CombatState battle;
    private final List<String> log = new ArrayList<>();

    public synchronized boolean hasBattle() {
        return battle != null;
    }

    public synchronized Map<String, Object> newBattle(String heroClassName, Long seedOrNull) {
        HeroClass choice;
        try {
            choice = HeroClass.valueOf(heroClassName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        }
        if (choice == HeroClass.NEUTRAL) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        }
        this.heroClass = choice;
        this.seed = seedOrNull != null ? seedOrNull : new Random().nextLong();
        this.log.clear();
        Combatant hero = Combatant.hero(HERO_NAME, choice, choice.startingHp());
        List<CardDef> deck = cards.starterDeck(choice);
        List<EnemyDef> foes = encounters.combat(1, new SplittableRandom(seed));
        this.battle = engine.newBattle(hero, List.of(), deck, foes, seed * 31 + 1);
        log.add(HERO_NAME + " (" + displayName(choice) + ") sails into the Act 1 sky-lanes.");
        log.add("Foes block the way: " + foeNames(foes) + ".");
        drainEvents();
        return snapshot();
    }

    public synchronized Map<String, Object> play(int handIndex, int target) {
        requireBattle();
        String name = handIndex >= 0 && handIndex < battle.hand().size()
                ? battle.hand().get(handIndex).name() : "card";
        engine.playCard(battle, handIndex, target);
        log.add("You play " + name + ".");
        drainEvents();
        checkOutcome();
        return snapshot();
    }

    public synchronized Map<String, Object> endTurn() {
        requireBattle();
        engine.endTurn(battle);
        log.add("You brace. The foes close in.");
        drainEvents();
        checkOutcome();
        return snapshot();
    }

    public synchronized Map<String, Object> snapshot() {
        if (battle == null) {
            Map<String, Object> select = new LinkedHashMap<>();
            select.put("phase", "select");
            select.put("heroes", heroOptions());
            return select;
        }
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("phase", battle.over() ? "over" : "battle");
        snap.put("label", "Act 1 — Sky-Lane Skirmish");
        snap.put("heroClass", heroClass.name());
        snap.put("heroName", HERO_NAME);
        snap.put("seed", seed);
        snap.put("turn", battle.turn());
        snap.put("energy", battle.energy());
        snap.put("hero", fighter(battle.hero()));
        List<Map<String, Object>> hand = new ArrayList<>();
        for (int i = 0; i < battle.hand().size(); i++) {
            hand.add(card(battle.hand().get(i), i, battle.energy(), battle.over()));
        }
        snap.put("hand", hand);
        List<Map<String, Object>> foes = new ArrayList<>();
        for (int i = 0; i < battle.enemies().size(); i++) {
            foes.add(foe(battle.enemies().get(i), foeDef(i), battle.intents().get(i), i));
        }
        snap.put("enemies", foes);
        snap.put("companions", List.of());
        snap.put("drawCount", battle.drawPile().size());
        snap.put("discardCount", battle.discardPile().size());
        snap.put("over", battle.over());
        snap.put("victory", battle.victory());
        snap.put("log", logTail());
        return snap;
    }

    private void requireBattle() {
        if (battle == null) {
            throw new IllegalStateException("no battle started");
        }
        if (battle.over()) {
            throw new IllegalStateException("battle is over");
        }
    }

    private void drainEvents() {
        for (String event : battle.events()) {
            log.add(event);
        }
        battle.events().clear();
        while (log.size() > LOG_CAP) {
            log.remove(0);
        }
    }

    private List<String> logTail() {
        return log.subList(Math.max(0, log.size() - LOG_TAIL), log.size());
    }

    private void checkOutcome() {
        if (battle.over()) {
            if (battle.victory()) {
                log.add("VICTORY in " + battle.turn() + " turns. The sky-lane is yours."
                        + " (Full campaign map sails in W2.)");
            } else {
                log.add("DEFEAT. The Guild will sing of " + HERO_NAME + ". Sail again.");
            }
        }
    }

    private EnemyDef foeDef(int index) {
        if (index < battle.enemyDefs().size()) {
            return battle.enemyDefs().get(index);
        }
        return null;
    }

    private static String foeNames(List<EnemyDef> foes) {
        List<String> names = foes.stream().map(EnemyDef::name).toList();
        return String.join(", ", names);
    }

    private static String displayName(HeroClass heroClass) {
        String lower = heroClass.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static List<Map<String, Object>> heroOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (HeroClass heroClass : HeroClass.values()) {
            if (heroClass == HeroClass.NEUTRAL) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", heroClass.name());
            option.put("name", displayName(heroClass));
            option.put("hp", heroClass.startingHp());
            option.put("aspect", heroClass.aspect().name());
            option.put("blurb", BLURBS.get(heroClass));
            options.add(option);
        }
        return options;
    }

    private static Map<String, Object> fighter(Combatant fighter) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", fighter.name());
        json.put("hp", fighter.hp());
        json.put("maxHp", fighter.maxHp());
        json.put("block", fighter.block());
        json.put("strength", fighter.strength());
        json.put("weak", fighter.weak());
        json.put("vulnerable", fighter.vulnerable());
        json.put("aspect", fighter.aspect().name());
        json.put("alive", fighter.alive());
        return json;
    }

    private static Map<String, Object> foe(Combatant foe, EnemyDef def, Intent intent, int index) {
        Map<String, Object> json = fighter(foe);
        json.put("index", index);
        json.put("row", foe.row().name());
        Map<String, Object> intentJson = new LinkedHashMap<>();
        intentJson.put("kind", intent.kind().name());
        intentJson.put("preview", intent.preview());
        json.put("intent", intentJson);
        if (def != null) {
            json.put("foeId", def.id());
            json.put("boss", def.boss());
            json.put("flavor", def.flavor());
        }
        return json;
    }

    private static Map<String, Object> card(CardDef card, int index, int energy, boolean over) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("index", index);
        json.put("id", card.id());
        json.put("name", card.name());
        json.put("cost", card.cost());
        json.put("type", card.type().name());
        json.put("aspect", card.aspect().name());
        json.put("text", rulesText(card));
        json.put("flavor", card.flavor());
        json.put("unplayable", card.unplayable());
        json.put("needsTarget", card.targetsEnemy() && !card.aoe());
        json.put("playable", !over && !card.unplayable() && card.cost() <= energy);
        return json;
    }

    static String rulesText(CardDef card) {
        if (card.unplayable()) {
            return "Unplayable. It clogs your hand.";
        }
        List<String> parts = new ArrayList<>();
        if (card.damage() > 0) {
            String hit = "Deal " + card.damage();
            if (card.hits() > 1) {
                hit += " x" + card.hits();
            }
            if (card.aoe()) {
                hit += " to ALL enemies";
            }
            parts.add(hit + ".");
        }
        if (card.block() > 0) {
            parts.add("Gain " + card.block() + " Block.");
        }
        if (card.heal() > 0) {
            parts.add("Heal " + card.heal() + ".");
        }
        if (card.draw() > 0) {
            parts.add("Draw " + card.draw() + ".");
        }
        if (card.energy() > 0) {
            parts.add("Gain " + card.energy() + " Energy.");
        }
        if (card.strength() > 0) {
            parts.add("Gain " + card.strength() + " Strength.");
        }
        if (card.weak() > 0) {
            parts.add("Apply " + card.weak() + " Weak.");
        }
        if (card.vulnerable() > 0) {
            parts.add("Apply " + card.vulnerable() + " Vulnerable.");
        }
        return parts.isEmpty() ? card.type().name() + "." : String.join(" ", parts);
    }
}
