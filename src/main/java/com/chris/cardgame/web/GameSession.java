package com.chris.cardgame.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SplittableRandom;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;

/**
 * One browser session's game state: either a quick skirmish battle or a full
 * campaign run. All methods are synchronized: the embedded server handles
 * requests on a small pool.
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
    private final Path saveFile;

    private HeroClass heroClass;
    private long seed;
    private CombatState battle;
    private WebRun run;
    private final List<String> log = new ArrayList<>();

    public GameSession() {
        this(SaveStore.defaultPath());
    }

    public GameSession(Path saveFile) {
        this.saveFile = saveFile;
    }

    public synchronized boolean hasBattle() {
        return battle != null;
    }

    public synchronized boolean hasSave() {
        return Files.exists(saveFile);
    }

    public synchronized Map<String, Object> newBattle(String heroClassName, Long seedOrNull) {
        HeroClass choice = parseHero(heroClassName);
        this.heroClass = choice;
        this.seed = seedOrNull != null ? seedOrNull : new Random().nextLong();
        this.log.clear();
        this.run = null;
        Combatant hero = Combatant.hero(HERO_NAME, choice, choice.startingHp());
        List<CardDef> deck = cards.starterDeck(choice);
        List<EnemyDef> foes = encounters.combat(1, new SplittableRandom(seed));
        this.battle = engine.newBattle(hero, List.of(), deck, foes, seed * 31 + 1);
        log.add(HERO_NAME + " (" + Snapshots.displayName(choice) + ") sails into a sky skirmish.");
        log.add("Foes block the way: " + foeNames(foes) + ".");
        drainEvents();
        return snapshot();
    }

    public synchronized Map<String, Object> newRun(String heroClassName, Long seedOrNull) {
        this.battle = null;
        this.log.clear();
        this.run = WebRun.start(heroClassName, seedOrNull, saveFile);
        return snapshot();
    }

    public synchronized Map<String, Object> continueRun() {
        this.battle = null;
        this.log.clear();
        this.run = WebRun.resume(saveFile);
        return snapshot();
    }

    public synchronized Map<String, Object> play(int handIndex, int target) {
        if (run != null && run.inBattle()) {
            return run.play(handIndex, target);
        }
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
        if (run != null && run.inBattle()) {
            return run.endTurn();
        }
        requireBattle();
        engine.endTurn(battle);
        log.add("You brace. The foes close in.");
        drainEvents();
        checkOutcome();
        return snapshot();
    }

    public synchronized Map<String, Object> chooseNode(String id) {
        return requireRun().chooseNode(id);
    }

    public synchronized Map<String, Object> chooseBoon(int index) {
        return requireRun().chooseBoon(index);
    }

    public synchronized Map<String, Object> chooseDraft(Integer index, boolean skip) {
        return requireRun().chooseDraft(index, skip);
    }

    public synchronized Map<String, Object> shopBuy(String kind, Integer index) {
        return requireRun().shopBuy(kind, index);
    }

    public synchronized Map<String, Object> shopLeave() {
        return requireRun().shopLeave();
    }

    public synchronized Map<String, Object> tavern(String action) {
        return requireRun().tavern(action);
    }

    public synchronized Map<String, Object> tavernLeave() {
        return requireRun().tavernLeave();
    }

    public synchronized Map<String, Object> eventChoose(int index) {
        return requireRun().eventChoose(index);
    }

    public synchronized Map<String, Object> abandon() {
        return requireRun().abandon();
    }

    public synchronized Map<String, Object> snapshot() {
        if (run != null) {
            return run.snapshot();
        }
        if (battle == null) {
            Map<String, Object> select = new LinkedHashMap<>();
            select.put("phase", "select");
            select.put("heroes", heroOptions());
            select.put("hasSave", hasSave());
            return select;
        }
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("mode", "skirmish");
        snap.put("phase", battle.over() ? "over" : "battle");
        snap.put("label", "Sky Skirmish");
        snap.put("heroClass", heroClass.name());
        snap.put("heroName", HERO_NAME);
        snap.put("seed", seed);
        snap.put("turn", battle.turn());
        snap.put("energy", battle.energy());
        snap.put("hero", Snapshots.fighter(battle.hero()));
        List<Map<String, Object>> hand = new ArrayList<>();
        for (int i = 0; i < battle.hand().size(); i++) {
            hand.add(Snapshots.card(battle.hand().get(i), i, battle.energy(), battle.over()));
        }
        snap.put("hand", hand);
        List<Map<String, Object>> foes = new ArrayList<>();
        for (int i = 0; i < battle.enemies().size(); i++) {
            EnemyDef def = i < battle.enemyDefs().size() ? battle.enemyDefs().get(i) : null;
            foes.add(Snapshots.foe(battle.enemies().get(i), def, battle.intents().get(i), i));
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

    private WebRun requireRun() {
        if (run == null) {
            throw new IllegalStateException("No campaign started.");
        }
        return run;
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
                log.add("VICTORY in " + battle.turn() + " turns. The sky-lane is yours.");
            } else {
                log.add("DEFEAT. The Guild will sing of " + HERO_NAME + ". Sail again.");
            }
        }
    }

    private static HeroClass parseHero(String heroClassName) {
        try {
            HeroClass choice = HeroClass.valueOf(heroClassName.trim().toUpperCase());
            if (choice == HeroClass.NEUTRAL) {
                throw new IllegalArgumentException("unknown hero: " + heroClassName);
            }
            return choice;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        }
    }

    private static String foeNames(List<EnemyDef> foes) {
        List<String> names = foes.stream().map(EnemyDef::name).toList();
        return String.join(", ", names);
    }

    private static List<Map<String, Object>> heroOptions() {
        List<Map<String, Object>> options = new ArrayList<>();
        for (HeroClass heroClass : HeroClass.values()) {
            if (heroClass == HeroClass.NEUTRAL) {
                continue;
            }
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("id", heroClass.name());
            option.put("name", Snapshots.displayName(heroClass));
            option.put("hp", heroClass.startingHp());
            option.put("aspect", heroClass.aspect().name());
            option.put("blurb", BLURBS.get(heroClass));
            options.add(option);
        }
        return options;
    }
}
