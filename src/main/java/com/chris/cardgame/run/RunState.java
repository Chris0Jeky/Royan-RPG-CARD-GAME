package com.chris.cardgame.run;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.loot.XpCurve;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.RelicDef;
import com.chris.cardgame.model.RelicEffect;

public class RunState {
    private static final List<String> BASIC_IDS = List.of(
            "knight-strike", "knight-guard", "ranger-aimed", "ranger-dodge",
            "runemage-spark", "runemage-ward");

    public static final int MAX_COMPANIONS = 2;

    private final Combatant hero;
    private final HeroClass heroClass;
    private final List<CardDef> deck;
    private final List<Companion> companions = new ArrayList<>();
    private final java.util.Set<String> seenEvents = new java.util.HashSet<>();
    private final List<RelicDef> relics = new ArrayList<>();
    private final SplittableRandom rng;
    private final long seed;
    private int gold;
    private int dust;
    private int shards;
    private int xp;
    private int level;
    private int act;

    public RunState(String heroName, HeroClass heroClass, List<CardDef> starterDeck, long seed) {
        this.hero = Combatant.hero(heroName, heroClass, heroClass.startingHp());
        this.heroClass = heroClass;
        this.deck = new ArrayList<>(starterDeck);
        this.seed = seed;
        this.rng = new SplittableRandom(seed);
        this.gold = 50;
        this.xp = 0;
        this.level = 1;
        this.act = 1;
    }

    public Combatant hero() {
        return hero;
    }

    public HeroClass heroClass() {
        return heroClass;
    }

    public List<CardDef> deck() {
        return deck;
    }

    public SplittableRandom rng() {
        return rng;
    }

    public long seed() {
        return seed;
    }

    public int gold() {
        return gold;
    }

    public int dust() {
        return dust;
    }

    public int shards() {
        return shards;
    }

    public List<RelicDef> relics() {
        return relics;
    }

    public void addDust(int amount) {
        dust += amount;
    }

    public boolean spendDust(int amount) {
        if (dust < amount) {
            return false;
        }
        dust -= amount;
        return true;
    }

    public void addShards(int amount) {
        shards += amount;
    }

    public boolean spendShards(int amount) {
        if (shards < amount) {
            return false;
        }
        shards -= amount;
        return true;
    }

    public void addRelic(RelicDef relic) {
        relics.add(relic);
        switch (relic.effect()) {
            case MAX_HP -> hero.raiseMaxHp(relic.value());
            case BASE_STRENGTH -> hero.gainBaseStrength(relic.value());
            case PLATING -> hero.gainPlating(relic.value());
            case FIRST_TURN_ENERGY -> hero.gainFirstTurnEnergy(relic.value());
            case FIRST_TURN_DRAW -> hero.gainFirstTurnDraw(relic.value());
            case GOLD_PCT, HEAL_AFTER_COMBAT -> {
            }
        }
    }

    public int goldPctBonus() {
        return relics.stream()
                .filter(relic -> relic.effect() == RelicEffect.GOLD_PCT)
                .mapToInt(RelicDef::value).sum();
    }

    public int healAfterCombat() {
        return relics.stream()
                .filter(relic -> relic.effect() == RelicEffect.HEAL_AFTER_COMBAT)
                .mapToInt(RelicDef::value).sum();
    }

    public boolean hasBasic() {
        return deck.stream().anyMatch(card -> BASIC_IDS.contains(card.id()));
    }

    public boolean removeBasic() {
        for (int i = 0; i < deck.size(); i++) {
            if (BASIC_IDS.contains(deck.get(i).id())) {
                deck.remove(i);
                return true;
            }
        }
        return false;
    }

    public SaveData toSave(String nodeId) {
        return new SaveData(1, seed, heroClass, act, nodeId,
                hero.hp(), hero.maxHp(), hero.strength(), hero.plating(),
                hero.firstTurnEnergy(), hero.firstTurnDraw(),
                deck.stream().map(CardDef::id).toList(),
                relics.stream().map(RelicDef::id).toList(),
                companions.stream().map(companion -> companion.def().id()).toList(),
                companions.stream().map(Companion::hp).toList(),
                gold, dust, shards, xp, level, java.util.Set.copyOf(seenEvents));
    }

    public static RunState fromSave(SaveData save, CardLoader cards, RelicLoader relics,
            CompanionLoader companions) {
        RunState state = new RunState("Captain Royan", save.heroClass(),
                save.deck().stream().map(cards::get).toList(), save.seed());
        state.setAct(save.act());
        state.hero().raiseMaxHp(save.heroMaxHp() - state.hero().maxHp());
        state.hero().takeDamage(state.hero().maxHp());
        state.hero().heal(save.heroHp());
        state.hero().gainBaseStrength(save.baseStrength());
        state.hero().gainPlating(save.plating());
        state.hero().gainFirstTurnEnergy(save.firstTurnEnergy());
        state.hero().gainFirstTurnDraw(save.firstTurnDraw());
        save.relics().forEach(id -> state.relics.add(relics.get(id)));
        for (int i = 0; i < save.companionIds().size(); i++) {
            Companion companion = new Companion(companions.get(save.companionIds().get(i)));
            companion.setHp(save.companionHp().get(i));
            state.companions.add(companion);
        }
        state.gold = save.gold();
        state.dust = save.dust();
        state.shards = save.shards();
        state.xp = save.xp();
        state.level = save.level();
        state.seenEvents.addAll(save.seenEvents());
        return state;
    }

    public int xp() {
        return xp;
    }

    public int level() {
        return level;
    }

    public int act() {
        return act;
    }

    public void setAct(int act) {
        this.act = act;
        this.seenEvents.clear();
    }

    public List<Companion> companions() {
        return companions;
    }

    public boolean recruit(Companion companion) {
        if (companions.size() >= MAX_COMPANIONS) {
            return false;
        }
        companions.add(companion);
        return true;
    }

    public java.util.Set<String> seenEvents() {
        return seenEvents;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public boolean spendGold(int amount) {
        if (gold < amount) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public boolean addCard(CardDef card) {
        if (deck.size() >= LootGen.MAX_DECK) {
            return false;
        }
        long copies = deck.stream().filter(c -> c.id().equals(card.id())).count();
        if (copies >= LootGen.MAX_COPIES) {
            return false;
        }
        deck.add(card);
        return true;
    }

    public List<Boon> addXp(int amount) {
        List<Boon> earned = new ArrayList<>();
        for (List<Boon> options : levelUp(amount)) {
            Boon pick = options.stream()
                    .sorted((a, b) -> Integer.compare(boonScore(b), boonScore(a)))
                    .findFirst().orElseThrow();
            applyBoon(pick);
            earned.add(pick);
        }
        return earned;
    }

    public List<List<Boon>> levelUp(int amount) {
        xp += amount;
        List<List<Boon>> offers = new ArrayList<>();
        while (level < XpCurve.MAX_LEVEL && xp >= XpCurve.xpForNext(level)) {
            xp -= XpCurve.xpForNext(level);
            level++;
            offers.add(Boon.offer(rng));
        }
        return offers;
    }

    private int boonScore(Boon boon) {
        boolean hurt = hero.hp() <= hero.maxHp() * 2 / 3;
        return switch (boon.id()) {
            case "juggernaut" -> 100;
            case "war-paint" -> 95;
            case "feast" -> hurt ? 90 : 20;
            case "second-wind-blessing" -> 70;
            default -> 50;
        };
    }

    public void applyBoon(Boon boon) {
        if (boon.maxHp() > 0) {
            hero.raiseMaxHp(boon.maxHp());
        }
        hero.heal(boon.heal());
        hero.gainBaseStrength(boon.strength());
        gold += boon.gold();
    }
}
