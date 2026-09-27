package com.chris.cardgame.web;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.chris.cardgame.cli.SaveStore;
import com.chris.cardgame.combat.CombatEngine;
import com.chris.cardgame.combat.CombatState;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.HeroLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.EncounterGen;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.loot.XpCurve;
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
import com.chris.cardgame.model.HeroDef;
import com.chris.cardgame.model.RelicDef;
import com.chris.cardgame.run.Companion;
import com.chris.cardgame.run.Events;
import com.chris.cardgame.run.RunEngine;
import com.chris.cardgame.run.RunState;
import com.chris.cardgame.run.SaveData;
import com.chris.cardgame.run.Shop;
import com.chris.cardgame.run.Tavern;

/**
 * A full 3-act campaign behind discrete HTTP steps. Mirrors
 * {@code InteractiveLoop} node-for-node (same rolls, same order, same
 * numbers); the only deliberate differences are web suspend/resume landing
 * back on the map instead of re-resolving a node, and shop purchases
 * validating before spending (per AGENTS.md).
 *
 * <p>All methods must be called under the owning session lock.
 */
public class WebRun {
    static final String MAP = "map";
    static final String BATTLE = "battle";
    static final String LEVELUP = "levelup";
    static final String DRAFT = "draft";
    static final String SHOP = "shop";
    static final String TAVERN = "tavern";
    static final String EVENT = "event";

    private static final int LOG_CAP = 300;
    private static final int LOG_TAIL = 80;

    private final CombatEngine engine = new CombatEngine();
    private final CardLoader cards = CardLoader.load();
    private final EnemyLoader enemies = EnemyLoader.load();
    private final RelicLoader relics = RelicLoader.load();
    private final CompanionLoader companions = CompanionLoader.load();
    private final EventLoader eventDefs = EventLoader.load();
    private final EncounterGen encounters = new EncounterGen(enemies);
    private final LootGen loot = new LootGen(cards);
    private final Events events = new Events(eventDefs, cards, relics, companions);
    private final MapGen maps = new MapGen();
    private final HeroLoader heroes = HeroLoader.load();
    private final Path saveFile;
    private final List<String> log = new ArrayList<>();

    private final List<Map<String, Object>> chronicle = new ArrayList<>();
    private Map<String, Object> banter;
    private RunState state;
    private ActMap map;
    private String nodeId;
    private String screen = MAP;
    private CombatState battle;
    private boolean battleElite;
    private final List<List<Boon>> boonOffers = new ArrayList<>();
    private List<CardDef> draftOptions = List.of();
    private int draftsLeft;
    private boolean draftElite;
    private EventDef event;
    private List<CardDef> shopStock = List.of();
    private RelicDef shopRelic;
    private boolean shopRelicSold;
    private Map<String, Object> arrival;
    private boolean over;
    private boolean victory;
    private boolean abandoned;

    private WebRun(Path saveFile) {
        this.saveFile = saveFile;
    }

    public static WebRun start(String heroClassName, Long seedOrNull, Path saveFile) {
        HeroClass choice;
        try {
            choice = HeroClass.valueOf(heroClassName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        }
        if (choice == HeroClass.NEUTRAL) {
            throw new IllegalArgumentException("unknown hero: " + heroClassName);
        }
        long seed = seedOrNull != null ? seedOrNull : new Random().nextLong();
        WebRun run = new WebRun(saveFile);
        run.state = new RunState(GameSession.HERO_NAME, choice,
                run.cards.starterDeck(choice), seed);
        run.map = run.maps.generate(1, seed * 31 + 1);
        run.nodeId = null;
        run.screen = MAP;
        run.log("Captain Royan (" + Snapshots.displayName(choice)
                + ") takes the Guild commission. Three acts. No way back but through.");
        HeroDef story = run.heroes.get(choice);
        run.log(story.origin());
        run.arrival = banner("The Commission", story.motive());
        run.chronicle("commission", "Captain Royan (" + Snapshots.displayName(choice)
                + ") takes the Guild commission.");
        return run;
    }

    public static WebRun resume(Path saveFile) {
        if (!Files.exists(saveFile)) {
            throw new IllegalStateException("No saved campaign.");
        }
        WebRun run = new WebRun(saveFile);
        SaveData save = SaveStore.load(saveFile);
        run.state = RunState.fromSave(save, run.cards, run.relics, run.companions);
        run.map = run.maps.generate(save.act(), run.state.seed() * 31 + save.act());
        run.nodeId = save.nodeId();
        if (run.nodeId != null && !run.map.nodes().containsKey(run.nodeId)) {
            run.nodeId = null;
        }
        run.screen = MAP;
        run.over = false;
        run.log("Campaign resumed: " + save.heroClass() + ", Act " + save.act()
                + ", level " + run.state.level() + ".");
        run.chronicle("resume", "The saga continues in Act " + save.act() + ".");
        return run;
    }

    public boolean inBattle() {
        return screen.equals(BATTLE) && battle != null && !battle.over();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("mode", "run");
        snap.put("phase", over ? "over" : "run");
        snap.put("screen", over ? "over" : screen);
        snap.put("label", "Act " + state.act() + " — "
                + (nodeId == null ? "Choosing a landing isle" : map.node(nodeId).type()));
        snap.put("seed", state.seed());
        snap.put("hero", Snapshots.fighter(state.hero()));
        snap.put("run", runView());
        snap.put("arrival", arrival);
        snap.put("over", over);
        snap.put("victory", victory);
        snap.put("abandoned", abandoned);
        snap.put("log", logTail());
        snap.put("chronicle", List.copyOf(chronicle));
        snap.put("banter", banter);
        banter = null;
        switch (screen) {
            case MAP -> snap.put("map", mapView());
            case BATTLE -> snap.putAll(battleView());
            case LEVELUP -> snap.put("levelup", levelupView());
            case DRAFT -> snap.put("draft", draftView());
            case SHOP -> snap.put("shop", shopView());
            case TAVERN -> snap.put("tavern", tavernView());
            case EVENT -> snap.put("event", eventView());
            default -> throw new IllegalStateException("bad screen: " + screen);
        }
        return snap;
    }

    public Map<String, Object> chooseNode(String id) {
        requireScreen(MAP);
        List<String> options = mapOptions();
        if (!options.contains(id)) {
            throw new IllegalArgumentException("That isle is off your course.");
        }
        arrival = null;
        nodeId = id;
        MapNode node = map.node(id);
        SaveStore.save(saveFile, state.toSave(nodeId));
        log("Sailing to " + id + " [" + node.type() + "].");
        switch (node.type()) {
            case COMBAT -> startBattle(encounters.combat(state.act(), state.rng()), false);
            case ELITE -> startBattle(encounters.elite(state.act(), state.rng()), true);
            case BOSS -> {
                List<EnemyDef> boss = encounters.boss(state.act());
                startBattle(boss, true);
                arrival = banner(boss.get(0).name(), boss.get(0).flavor());
                banterFromCompany();
            }
            case REST -> {
                int heal = Math.max(1, state.hero().maxHp() * 35 / 100);
                state.hero().heal(heal);
                log("Rested: +" + heal + " HP.");
                arrival = banner("A quiet cove", "The crew rests. +" + heal + " HP.");
                screen = MAP;
            }
            case SHOP -> {
                shopStock = new ArrayList<>(
                        loot.cardOptions(state.heroClass(), state.deck(), true, state.rng()));
                Set<String> owned = ownedRelics();
                shopRelic = relics.offer(owned, false, state.rng()).orElse(null);
                shopRelicSold = false;
                screen = SHOP;
            }
            case TAVERN -> screen = TAVERN;
            case EVENT -> {
                event = events.pick(state);
                screen = EVENT;
            }
        }
        return snapshot();
    }

    public Map<String, Object> play(int handIndex, int target) {
        requireBattle();
        String name = handIndex >= 0 && handIndex < battle.hand().size()
                ? battle.hand().get(handIndex).name() : "card";
        engine.playCard(battle, handIndex, target);
        log("You play " + name + ".");
        drainEvents();
        if (battle.over()) {
            finishBattle();
        }
        return snapshot();
    }

    public Map<String, Object> endTurn() {
        requireBattle();
        engine.endTurn(battle);
        log("You brace. The foes close in.");
        drainEvents();
        if (battle.over()) {
            finishBattle();
        }
        return snapshot();
    }

    public Map<String, Object> chooseBoon(int index) {
        requireScreen(LEVELUP);
        if (boonOffers.isEmpty()) {
            throw new IllegalStateException("No boon awaits.");
        }
        List<Boon> offer = boonOffers.get(0);
        if (index < 0 || index >= offer.size()) {
            throw new IllegalArgumentException("No such boon.");
        }
        state.applyBoon(offer.get(index));
        log("Boon claimed: " + offer.get(index).name() + ".");
        boonOffers.remove(0);
        arrival = null;
        if (!boonOffers.isEmpty()) {
            screen = LEVELUP;
        } else {
            startDrafts();
        }
        return snapshot();
    }

    public Map<String, Object> chooseDraft(Integer index, boolean skip) {
        requireScreen(DRAFT);
        if (skip) {
            state.addDust(RunEngine.SALVAGE_DUST);
            log("Draft salvaged: +" + RunEngine.SALVAGE_DUST + " dust.");
        } else {
            if (index == null || index < 0 || index >= draftOptions.size()) {
                throw new IllegalArgumentException("No such draft.");
            }
            if (state.addCard(draftOptions.get(index))) {
                log("Drafted: " + draftOptions.get(index).name() + ".");
            } else {
                log("Deck is full; the draft slips away.");
            }
        }
        arrival = null;
        draftsLeft--;
        if (draftsLeft > 0) {
            draftOptions = loot.cardOptions(state.heroClass(), state.deck(),
                    draftElite, state.rng());
            if (draftOptions.isEmpty()) {
                log("No draft options (collection exhausted).");
                postNode();
            } else {
                screen = DRAFT;
            }
        } else {
            postNode();
        }
        return snapshot();
    }

    public Map<String, Object> shopBuy(String kind, Integer index) {
        requireScreen(SHOP);
        switch (kind) {
            case "heal" -> {
                if (!state.spendGold(Shop.HEAL_COST)) {
                    throw new IllegalStateException("Not enough gold.");
                }
                state.hero().heal(Shop.HEAL_AMOUNT);
                log("Healed " + Shop.HEAL_AMOUNT + " HP.");
            }
            case "relic" -> {
                if (shopRelic == null || shopRelicSold) {
                    throw new IllegalStateException("No relic for sale.");
                }
                if (!state.spendGold(Shop.relicPrice(shopRelic))) {
                    throw new IllegalStateException("Not enough gold.");
                }
                state.addRelic(shopRelic);
                shopRelicSold = true;
                log("Bought relic: " + shopRelic.name() + ".");
                chronicle("relic", "Bought relic: " + shopRelic.name() + ".");
            }
            case "card" -> {
                if (index == null || index < 0 || index >= shopStock.size()) {
                    throw new IllegalArgumentException("No such card.");
                }
                CardDef card = shopStock.get(index);
                long copies = state.deck().stream()
                        .filter(owned -> owned.id().equals(card.id())).count();
                if (state.deck().size() >= LootGen.MAX_DECK || copies >= LootGen.MAX_COPIES) {
                    throw new IllegalStateException("Deck is full.");
                }
                if (!state.spendGold(Shop.price(card))) {
                    throw new IllegalStateException("Not enough gold.");
                }
                state.addCard(card);
                shopStock.remove((int) index);
                log("Bought card: " + card.name() + ".");
            }
            default -> throw new IllegalArgumentException("The shopkeep squints. Buy what?");
        }
        return snapshot();
    }

    public Map<String, Object> shopLeave() {
        requireScreen(SHOP);
        screen = MAP;
        log("You leave the shop.");
        return snapshot();
    }

    public Map<String, Object> tavern(String action) {
        requireScreen(TAVERN);
        switch (action) {
            case "meal" -> {
                if (!state.spendGold(Tavern.HEAL_COST)) {
                    throw new IllegalStateException("Not enough gold.");
                }
                int heal = state.hero().maxHp() / 2;
                state.hero().heal(heal);
                log("Hearty meal: +" + heal + " HP.");
            }
            case "remove" -> {
                if (state.dust() < Tavern.REMOVE_COST_DUST || !state.hasBasic()
                        || !state.spendDust(Tavern.REMOVE_COST_DUST) || !state.removeBasic()) {
                    throw new IllegalStateException(
                            "Need " + Tavern.REMOVE_COST_DUST + " dust and a basic card.");
                }
                log("Struck a basic card from the deck.");
            }
            case "recruit" -> {
                if (state.companions().size() >= RunState.MAX_COMPANIONS) {
                    throw new IllegalStateException("War-band is full.");
                }
                if (!state.spendGold(Tavern.RECRUIT_COST)) {
                    throw new IllegalStateException("Not enough gold.");
                }
                int warband = state.companions().size();
                capture(out -> Events.recruit(state, companions, out));
                if (state.companions().size() > warband) {
                    Companion recruit = state.companions().get(state.companions().size() - 1);
                    chronicle("recruit", recruit.def().name() + " joined the war-band.");
                    banterFrom(recruit);
                }
            }
            case "relic" -> {
                if (state.shards() < Tavern.RELIC_COST_SHARDS
                        || state.gold() < Tavern.RELIC_COST_GOLD
                        || !state.spendShards(Tavern.RELIC_COST_SHARDS)
                        || !state.spendGold(Tavern.RELIC_COST_GOLD)) {
                    throw new IllegalStateException("Need " + Tavern.RELIC_COST_GOLD + "g + "
                            + Tavern.RELIC_COST_SHARDS + " shard.");
                }
                relics.offer(ownedRelics(), true, state.rng()).ifPresentOrElse(
                        relic -> {
                            state.addRelic(relic);
                            log("Traded for relic: " + relic.name() + ".");
                            chronicle("relic", "Traded for relic: " + relic.name() + ".");
                        },
                        () -> log("Relic vaults empty."));
            }
            default -> throw new IllegalArgumentException("The tavern offers no such comfort.");
        }
        return snapshot();
    }

    public Map<String, Object> tavernLeave() {
        requireScreen(TAVERN);
        screen = MAP;
        log("You settle your tab and leave the tavern.");
        return snapshot();
    }

    public Map<String, Object> eventChoose(int index) {
        requireScreen(EVENT);
        if (index < 0 || index >= event.choices().size()) {
            throw new IllegalArgumentException("No such choice.");
        }
        EventChoice choice = event.choices().get(index);
        if (!affordable(choice)) {
            throw new IllegalStateException("You cannot afford that choice.");
        }
        log("Chose: " + choice.text());
        chronicle("event", "At " + event.title() + ": " + choice.text());
        int warband = state.companions().size();
        capture(out -> events.apply(state, choice, out));
        if (state.companions().size() > warband) {
            Companion recruit = state.companions().get(state.companions().size() - 1);
            chronicle("recruit", recruit.def().name() + " joined the war-band.");
            banterFrom(recruit);
        }
        if (!state.hero().alive()) {
            return defeat("The event proved fatal.");
        }
        screen = MAP;
        return snapshot();
    }

    public Map<String, Object> abandon() {
        if (over) {
            throw new IllegalStateException("The campaign is already over.");
        }
        if (!screen.equals(MAP)) {
            throw new IllegalStateException("Finish this isle before suspending the campaign.");
        }
        SaveStore.save(saveFile, state.toSave(nodeId));
        over = true;
        abandoned = true;
        log("Campaign suspended. The Guild holds your charts.");
        chronicle("suspend", "Campaign suspended in Act " + state.act() + ".");
        return snapshot();
    }

    private void startBattle(List<EnemyDef> foes, boolean elite) {
        List<Combatant> fighters = state.companions().stream()
                .map(Companion::toCombatant).toList();
        List<CompanionRole> roles = state.companions().stream()
                .map(companion -> companion.def().role()).toList();
        List<Integer> powers = state.companions().stream()
                .map(companion -> companion.def().power()).toList();
        battle = engine.newBattle(state.hero(), fighters, roles, powers,
                state.deck(), foes, state.rng().nextLong());
        battleElite = elite;
        screen = BATTLE;
        log("Battle! " + foes.size() + " foe(s) bar the way.");
        drainEvents();
    }

    private void finishBattle() {
        List<Integer> companionHp = battle.companions().stream().map(Combatant::hp).toList();
        List<String> warbandBefore = state.companions().stream()
                .map(companion -> companion.def().name()).toList();
        capture(out -> RunEngine.syncCompanions(state, companionHp, out));
        List<String> warbandAfter = state.companions().stream()
                .map(companion -> companion.def().name()).toList();
        warbandBefore.stream().filter(name -> !warbandAfter.contains(name))
                .forEach(name -> chronicle("loss", name + " fell in battle."));
        if (!battle.victory()) {
            defeat("The Captain falls in Act " + state.act() + ".");
            return;
        }
        log("VICTORY in " + battle.turn() + " turns.");
        String foeNames = battle.enemyDefs().stream().map(EnemyDef::name)
                .collect(Collectors.joining(", "));
        chronicle(battleElite ? "elite" : "battle", "Won at " + nodeId + " — slew " + foeNames + ".");
        banterFromCompany();
        int patchUp = state.healAfterCombat();
        if (patchUp > 0) {
            state.hero().heal(patchUp);
            log("Relics mend " + patchUp + " HP.");
        }
        List<EnemyDef> foes = battle.enemyDefs();
        boolean boss = map.node(nodeId).type()
                == com.chris.cardgame.map.NodeType.BOSS;
        int base = foes.stream()
                .mapToInt(foe -> loot.rollGold(foe.goldMin(), foe.goldMax(), state.rng()))
                .sum() + (battleElite ? 25 : 0);
        int gold = base * (100 + state.goldPctBonus()) / 100;
        int xp = foes.stream().mapToInt(EnemyDef::xp).sum();
        state.addGold(gold);
        log("Spoils: +" + gold + " gold, +" + xp + " XP.");
        StringBuilder spoils = new StringBuilder("+" + gold + " gold, +" + xp + " XP");
        if (battleElite) {
            int shards = boss ? 2 : 1;
            state.addShards(shards);
            log("Claimed " + shards + " sky-shard(s).");
            spoils.append(", +").append(shards).append(" shards");
            relics.offer(ownedRelics(), true, state.rng()).ifPresentOrElse(
                    relic -> {
                        state.addRelic(relic);
                        log("Relic claimed: " + relic.name() + ".");
                        chronicle("relic", "Claimed relic: " + relic.name() + ".");
                        spoils.append(", relic: ").append(relic.name());
                    },
                    () -> {
                        state.addGold(50);
                        log("Relic vaults empty: +50 gold instead.");
                        spoils.append(", +50 gold");
                    });
        }
        arrival = banner("Spoils of battle", spoils.toString());
        boonOffers.clear();
        boonOffers.addAll(state.levelUp(xp));
        if (!boonOffers.isEmpty()) {
            log("Level " + state.level() + "! Choose a boon.");
            chronicle("level", "Reached level " + state.level() + ".");
            screen = LEVELUP;
        } else {
            startDrafts();
        }
    }

    private void startDrafts() {
        draftsLeft = battleElite ? 2 : 1;
        draftElite = battleElite;
        draftOptions = loot.cardOptions(state.heroClass(), state.deck(),
                draftElite, state.rng());
        if (draftOptions.isEmpty()) {
            log("No draft options (collection exhausted).");
            postNode();
        } else {
            screen = DRAFT;
        }
    }

    private void postNode() {
        boolean boss = map.node(nodeId).type()
                == com.chris.cardgame.map.NodeType.BOSS;
        if (!boss) {
            screen = MAP;
            return;
        }
        int heal = state.hero().maxHp() * 2 / 5;
        state.hero().heal(heal);
        log("Act " + state.act() + " cleared! +" + heal + " HP.");
        chronicle("act", "Cleared Act " + state.act() + ".");
        if (state.act() >= 3) {
            over = true;
            victory = true;
            SaveStore.delete(saveFile);
            log("CAMPAIGN VICTORY: the Sky-Tyrant falls!");
            chronicle("victory", "The Sky-Tyrant falls. Legend.");
            arrival = banner("The Sky-Tyrant falls",
                    "Three acts, one legend. The Guild drinks to " + GameSession.HERO_NAME + ".");
            return;
        }
        state.setAct(state.act() + 1);
        map = maps.generate(state.act(), state.seed() * 31 + state.act());
        nodeId = null;
        screen = MAP;
        arrival = banner("Act " + state.act(),
                "New skies, harder foes. Choose your landing isle.");
        SaveStore.save(saveFile, state.toSave(null));
    }

    private Map<String, Object> defeat(String line) {
        over = true;
        victory = false;
        SaveStore.delete(saveFile);
        log(line);
        chronicle("defeat", line);
        arrival = banner("Defeat", line);
        return snapshot();
    }

    private void requireScreen(String want) {
        if (over) {
            throw new IllegalStateException("The campaign is over.");
        }
        if (!screen.equals(want)) {
            throw new IllegalStateException("That choice is not on this isle.");
        }
    }

    private void requireBattle() {
        if (over) {
            throw new IllegalStateException("The campaign is over.");
        }
        if (!inBattle()) {
            throw new IllegalStateException("No battle rages.");
        }
    }

    private List<String> mapOptions() {
        if (nodeId == null) {
            return map.entries();
        }
        return map.node(nodeId).children();
    }

    private Set<String> ownedRelics() {
        return state.relics().stream().map(RelicDef::id).collect(Collectors.toSet());
    }

    private boolean affordable(EventChoice choice) {
        if (choice.requires() == null) {
            return true;
        }
        return state.gold() >= choice.requires().gold()
                && state.dust() >= choice.requires().dust()
                && state.shards() >= choice.requires().shards()
                && state.hero().hp() > choice.requires().hp();
    }

    private void drainEvents() {
        for (String event : battle.events()) {
            log(event);
        }
        battle.events().clear();
    }

    private void capture(Consumer<PrintStream> call) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        call.accept(out);
        out.flush();
        for (String line : bytes.toString(StandardCharsets.UTF_8).split("\n")) {
            String clean = line.strip();
            if (!clean.isEmpty()) {
                log(clean);
            }
        }
    }

    private void chronicle(String kind, String text) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("act", state.act());
        entry.put("kind", kind);
        entry.put("text", text);
        chronicle.add(entry);
        while (chronicle.size() > 60) {
            chronicle.remove(0);
        }
    }

    private void banterFrom(Companion companion) {
        List<String> lines = companion.def().banter();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        String text = lines.get(state.rng().nextInt(lines.size()));
        Map<String, Object> bubble = new LinkedHashMap<>();
        bubble.put("speaker", companion.def().name());
        bubble.put("role", companion.def().role().name());
        bubble.put("text", text);
        banter = bubble;
        log(companion.def().name() + ": \"" + text + "\"");
    }

    private void banterFromCompany() {
        if (state.companions().isEmpty()) {
            return;
        }
        banterFrom(state.companions().get(state.rng().nextInt(state.companions().size())));
    }

    private void log(String line) {
        log.add(line);
        while (log.size() > LOG_CAP) {
            log.remove(0);
        }
    }

    private List<String> logTail() {
        return log.subList(Math.max(0, log.size() - LOG_TAIL), log.size());
    }

    private static Map<String, Object> banner(String title, String text) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("title", title);
        json.put("text", text);
        return json;
    }

    private Map<String, Object> runView() {
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("heroClass", state.heroClass().name());
        run.put("heroName", GameSession.HERO_NAME);
        HeroDef story = heroes.get(state.heroClass());
        Map<String, Object> storyView = new LinkedHashMap<>();
        storyView.put("title", story.title());
        storyView.put("origin", story.origin());
        storyView.put("motive", story.motive());
        storyView.put("triumph", story.triumph());
        storyView.put("epitaph", story.epitaph());
        run.put("story", storyView);
        run.put("act", state.act());
        run.put("gold", state.gold());
        run.put("dust", state.dust());
        run.put("shards", state.shards());
        run.put("level", state.level());
        run.put("xp", state.xp());
        run.put("xpNext", XpCurve.xpForNext(state.level()));
        run.put("deckSize", state.deck().size());
        run.put("deck", state.deck().stream().map(Snapshots::deckEntry).toList());
        run.put("relics", state.relics().stream().map(Snapshots::relicView).toList());
        run.put("companions",
                state.companions().stream().map(Snapshots::companionView).toList());
        run.put("maxCompanions", RunState.MAX_COMPANIONS);
        if (nodeId != null) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", nodeId);
            node.put("type", map.node(nodeId).type().name());
            run.put("node", node);
        } else {
            run.put("node", null);
        }
        return run;
    }

    private Map<String, Object> mapView() {
        Map<String, Object> view = new LinkedHashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (MapNode node : map.nodes().values()) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", node.id());
            json.put("layer", node.layer());
            json.put("type", node.type().name());
            json.put("children", node.children());
            nodes.add(json);
        }
        view.put("nodes", nodes);
        view.put("options", mapOptions());
        view.put("currentId", nodeId);
        view.put("bossId", map.bossId());
        return view;
    }

    private Map<String, Object> battleView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("turn", battle.turn());
        view.put("energy", battle.energy());
        List<Map<String, Object>> hand = new ArrayList<>();
        for (int i = 0; i < battle.hand().size(); i++) {
            hand.add(Snapshots.card(battle.hand().get(i), i, battle.energy(), battle.over()));
        }
        view.put("hand", hand);
        List<Map<String, Object>> foes = new ArrayList<>();
        for (int i = 0; i < battle.enemies().size(); i++) {
            EnemyDef def = i < battle.enemyDefs().size() ? battle.enemyDefs().get(i) : null;
            foes.add(Snapshots.foe(battle.enemies().get(i), def, battle.intents().get(i), i));
        }
        view.put("enemies", foes);
        List<Map<String, Object>> allies = new ArrayList<>();
        for (int i = 0; i < battle.companions().size(); i++) {
            allies.add(Snapshots.ally(battle.companions().get(i), i));
        }
        view.put("companions", allies);
        view.put("drawCount", battle.drawPile().size());
        view.put("discardCount", battle.discardPile().size());
        return view;
    }

    private Map<String, Object> levelupView() {
        Map<String, Object> view = new LinkedHashMap<>();
        List<Boon> offer = boonOffers.isEmpty() ? List.of() : boonOffers.get(0);
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < offer.size(); i++) {
            options.add(Snapshots.boonView(offer.get(i), i));
        }
        view.put("offer", options);
        view.put("pending", boonOffers.size());
        view.put("level", state.level());
        return view;
    }

    private Map<String, Object> draftView() {
        Map<String, Object> view = new LinkedHashMap<>();
        List<Map<String, Object>> options = new ArrayList<>();
        for (int i = 0; i < draftOptions.size(); i++) {
            options.add(Snapshots.card(draftOptions.get(i), i, 0, true));
        }
        view.put("options", options);
        view.put("left", draftsLeft);
        view.put("salvage", RunEngine.SALVAGE_DUST);
        return view;
    }

    private Map<String, Object> shopView() {
        Map<String, Object> view = new LinkedHashMap<>();
        List<Map<String, Object>> stock = new ArrayList<>();
        for (int i = 0; i < shopStock.size(); i++) {
            CardDef card = shopStock.get(i);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("card", Snapshots.card(card, i, 0, true));
            entry.put("price", Shop.price(card));
            stock.add(entry);
        }
        view.put("stock", stock);
        if (shopRelic != null && !shopRelicSold) {
            Map<String, Object> relic = new LinkedHashMap<>();
            relic.put("relic", Snapshots.relicView(shopRelic));
            relic.put("price", Shop.relicPrice(shopRelic));
            view.put("relic", relic);
        } else {
            view.put("relic", null);
        }
        view.put("healCost", Shop.HEAL_COST);
        view.put("healAmount", Shop.HEAL_AMOUNT);
        return view;
    }

    private Map<String, Object> tavernView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("mealCost", Tavern.HEAL_COST);
        view.put("removeCost", Tavern.REMOVE_COST_DUST);
        view.put("relicGold", Tavern.RELIC_COST_GOLD);
        view.put("relicShards", Tavern.RELIC_COST_SHARDS);
        view.put("recruitCost", Tavern.RECRUIT_COST);
        view.put("canRemove", state.dust() >= Tavern.REMOVE_COST_DUST && state.hasBasic());
        return view;
    }

    private Map<String, Object> eventView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("title", event.title());
        view.put("text", event.text());
        List<Map<String, Object>> choices = new ArrayList<>();
        for (int i = 0; i < event.choices().size(); i++) {
            EventChoice choice = event.choices().get(i);
            choices.add(Snapshots.choiceView(choice, i, affordable(choice)));
        }
        view.put("choices", choices);
        return view;
    }
}
