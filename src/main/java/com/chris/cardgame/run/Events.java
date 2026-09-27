package com.chris.cardgame.run;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.loot.LootGen;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.CompanionDef;
import com.chris.cardgame.model.EventDef;
import com.chris.cardgame.model.EventDef.EventChoice;
import com.chris.cardgame.model.EventDef.EventCost;
import com.chris.cardgame.model.EventDef.EventEffect;
import com.chris.cardgame.model.RelicDef;

public class Events {
    private final EventLoader events;
    private final CardLoader cards;
    private final LootGen loot;
    private final RelicLoader relics;
    private final CompanionLoader companions;

    public Events(EventLoader events, CardLoader cards, RelicLoader relics,
            CompanionLoader companions) {
        this.events = events;
        this.cards = cards;
        this.loot = new LootGen(cards);
        this.relics = relics;
        this.companions = companions;
    }

    public void resolve(RunState state, PrintStream out) {
        EventDef event = pick(state);
        EventChoice choice = choose(event, state);
        out.println("  Event: " + event.title() + " - " + event.text());
        out.println("  Chose: " + choice.text());
        apply(state, choice, out);
    }

    public EventDef pick(RunState state) {
        List<EventDef> fresh = events.all().stream()
                .filter(event -> !state.seenEvents().contains(event.id()))
                .filter(event -> event.requiresSeen() == null
                        || state.seenEvents().contains(event.requiresSeen()))
                .toList();
        if (fresh.isEmpty()) {
            state.seenEvents().clear();
            fresh = events.all().stream()
                    .filter(event -> event.requiresSeen() == null)
                    .toList();
        }
        EventDef picked = fresh.get(state.rng().nextInt(fresh.size()));
        state.seenEvents().add(picked.id());
        return picked;
    }

    public EventChoice choose(EventDef event, RunState state) {
        List<EventChoice> affordable = event.choices().stream()
                .filter(choice -> affordable(choice, state))
                .toList();
        if (affordable.isEmpty()) {
            return new EventChoice("leave", "Walk away.",
                    new EventCost(0, 0, 0, 0), new EventEffect(0, 0, 0, 0, 0, 0, 0,
                            false, false, false, false, false));
        }
        List<EventChoice> survivable = affordable.stream()
                .filter(choice -> effects(choice).damage() < state.hero().hp())
                .toList();
        List<EventChoice> pool = survivable.isEmpty() ? affordable : survivable;
        return pool.stream()
                .max(Comparator.comparingInt(choice -> score(choice, state)))
                .orElseThrow();
    }

    public void apply(RunState state, EventChoice choice, PrintStream out) {
        EventCost cost = choice.requires() == null ? new EventCost(0, 0, 0, 0) : choice.requires();
        state.spendGold(cost.gold());
        state.spendDust(cost.dust());
        state.spendShards(cost.shards());
        EventEffect fx = effects(choice);
        state.addGold(fx.gold());
        state.addDust(fx.dust());
        state.addShards(fx.shards());
        state.hero().heal(fx.heal());
        state.hero().takeDamage(fx.damage());
        if (fx.maxHp() > 0) {
            state.hero().raiseMaxHp(fx.maxHp());
        }
        state.hero().gainBaseStrength(fx.strength());
        if (fx.gold() != 0 || fx.dust() != 0 || fx.shards() != 0 || fx.heal() != 0
                || fx.damage() != 0 || fx.maxHp() != 0 || fx.strength() != 0) {
            out.println("  (" + describe(fx) + ")");
        }
        if (fx.draft()) {
            List<CardDef> options = loot.cardOptions(state.heroClass(), state.deck(), false, state.rng());
            options.stream().max(Comparator.comparingInt(card -> card.rarity().ordinal()))
                    .ifPresentOrElse(
                            pick -> {
                                if (state.addCard(pick)) {
                                    out.println("  Gained card: " + pick.name() + ".");
                                }
                            },
                            () -> out.println("  No card to gain."));
        }
        if (fx.relic()) {
            Set<String> owned = state.relics().stream()
                    .map(RelicDef::id).collect(Collectors.toSet());
            relics.offer(owned, false, state.rng()).ifPresentOrElse(
                    relic -> {
                        state.addRelic(relic);
                        out.println("  Gained relic: " + relic.name() + ".");
                    },
                    () -> {
                        state.addGold(50);
                        out.println("  Relic vaults empty: +50 gold instead.");
                    });
        }
        if (fx.curse()) {
            CardDef curse = state.rng().nextBoolean()
                    ? lootCard("curse-doubt") : lootCard("curse-sloth");
            state.deck().add(curse);
            out.println("  Cursed: " + curse.name() + " slips into the deck.");
        }
        if (fx.removeBasic()) {
            if (state.removeBasic()) {
                out.println("  Struck a basic card from the deck.");
            }
        }
        if (fx.companion()) {
            recruit(state, companions, out);
        }
    }

    public static void recruit(RunState state, CompanionLoader companions, PrintStream out) {
        Set<String> owned = state.companions().stream()
                .map(companion -> companion.def().id()).collect(Collectors.toSet());
        List<CompanionDef> free = companions.all().stream()
                .filter(def -> !owned.contains(def.id()))
                .toList();
        if (state.companions().size() >= RunState.MAX_COMPANIONS || free.isEmpty()) {
            state.addGold(40);
            out.println("  No room in the war-band: took 40 gold instead.");
            return;
        }
        CompanionDef def = free.get(state.rng().nextInt(free.size()));
        state.recruit(new Companion(def));
        out.println("  Recruited: " + def.name() + " (" + def.role() + ").");
    }

    private boolean affordable(EventChoice choice, RunState state) {
        EventCost cost = choice.requires() == null ? new EventCost(0, 0, 0, 0) : choice.requires();
        return state.gold() >= cost.gold()
                && state.dust() >= cost.dust()
                && state.shards() >= cost.shards()
                && state.hero().hp() > cost.hp();
    }

    private EventEffect effects(EventChoice choice) {
        return choice.effects() == null
                ? new EventEffect(0, 0, 0, 0, 0, 0, 0, false, false, false, false, false)
                : choice.effects();
    }

    private int score(EventChoice choice, RunState state) {
        EventEffect fx = effects(choice);
        int missing = state.hero().maxHp() - state.hero().hp();
        int value = 0;
        if (fx.relic()) {
            value += 100;
        }
        if (fx.companion()) {
            value += 90;
        }
        if (fx.draft()) {
            value += 60;
        }
        if (fx.removeBasic()) {
            value += 55;
        }
        value += fx.maxHp() * 6;
        value += fx.strength() * 35;
        value += fx.shards() * 40;
        value += Math.min(50, Math.min(fx.heal(), missing) * 2);
        value += fx.gold() / 3;
        value += fx.dust() / 4;
        value -= fx.damage() * 3;
        if (fx.curse()) {
            value -= 25;
        }
        return value;
    }

    private String describe(EventEffect fx) {
        StringBuilder sb = new StringBuilder();
        append(sb, fx.gold(), "gold");
        append(sb, fx.dust(), "dust");
        append(sb, fx.shards(), "shards");
        append(sb, fx.heal(), "heal");
        append(sb, -fx.damage(), "HP");
        append(sb, fx.maxHp(), "max HP");
        append(sb, fx.strength(), "strength");
        return sb.toString();
    }

    private void append(StringBuilder sb, int amount, String label) {
        if (amount > 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("+").append(amount).append(" ").append(label);
        } else if (amount < 0) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(amount).append(" ").append(label);
        }
    }

    private CardDef lootCard(String id) {
        return cards.get(id);
    }
}
