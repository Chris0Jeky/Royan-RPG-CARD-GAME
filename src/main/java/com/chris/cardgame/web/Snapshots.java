package com.chris.cardgame.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.chris.cardgame.combat.Intent;
import com.chris.cardgame.loot.Boon;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.EventDef.EventChoice;
import com.chris.cardgame.model.EventDef.EventCost;
import com.chris.cardgame.model.EventDef.EventEffect;
import com.chris.cardgame.model.HeroClass;
import com.chris.cardgame.model.RelicDef;
import com.chris.cardgame.run.Companion;

/** Shared JSON builders for skirmish and campaign snapshots. */
public final class Snapshots {
    private Snapshots() {
    }

    public static String displayName(HeroClass heroClass) {
        String lower = heroClass.name().toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static Map<String, Object> fighter(Combatant fighter) {
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

    public static Map<String, Object> foe(Combatant foe, EnemyDef def, Intent intent, int index) {
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

    public static Map<String, Object> ally(Combatant ally, int index) {
        Map<String, Object> json = fighter(ally);
        json.put("index", index);
        return json;
    }

    public static Map<String, Object> card(CardDef card, int index, int energy, boolean over) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("index", index);
        json.put("id", card.id());
        json.put("name", card.name());
        json.put("cost", card.cost());
        json.put("type", card.type().name());
        json.put("aspect", card.aspect().name());
        json.put("rarity", card.rarity().name());
        json.put("text", rulesText(card));
        json.put("flavor", card.flavor());
        json.put("unplayable", card.unplayable());
        json.put("needsTarget", card.targetsEnemy() && !card.aoe());
        json.put("playable", !over && !card.unplayable() && card.cost() <= energy);
        return json;
    }

    public static Map<String, Object> deckEntry(CardDef card) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", card.id());
        json.put("name", card.name());
        json.put("cost", card.cost());
        json.put("type", card.type().name());
        return json;
    }

    public static Map<String, Object> relicView(RelicDef relic) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", relic.id());
        json.put("name", relic.name());
        json.put("rarity", relic.rarity().name());
        json.put("effect", relic.effect().name());
        json.put("value", relic.value());
        json.put("flavor", relic.flavor());
        return json;
    }

    public static Map<String, Object> companionView(Companion companion) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", companion.def().id());
        json.put("name", companion.def().name());
        json.put("role", companion.def().role().name());
        json.put("aspect", companion.def().aspect().name());
        json.put("hp", companion.hp());
        json.put("maxHp", companion.def().hp());
        json.put("power", companion.def().power());
        json.put("flavor", companion.def().flavor());
        return json;
    }

    public static Map<String, Object> boonView(Boon boon, int index) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("index", index);
        json.put("id", boon.id());
        json.put("name", boon.name());
        json.put("desc", boon.desc());
        return json;
    }

    public static Map<String, Object> choiceView(EventChoice choice, int index, boolean affordable) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("index", index);
        json.put("text", choice.text());
        if (choice.requires() != null) {
            Map<String, Object> requires = new LinkedHashMap<>();
            requires.put("gold", choice.requires().gold());
            requires.put("dust", choice.requires().dust());
            requires.put("shards", choice.requires().shards());
            requires.put("hp", choice.requires().hp());
            json.put("requires", requires);
        } else {
            json.put("requires", null);
        }
        json.put("affordable", affordable);
        json.put("effects", effectsBlurb(choice.effects()));
        return json;
    }

    public static String effectsBlurb(EventEffect fx) {
        if (fx == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (fx.gold() > 0) {
            parts.add("+" + fx.gold() + " gold");
        }
        if (fx.dust() > 0) {
            parts.add("+" + fx.dust() + " dust");
        }
        if (fx.shards() > 0) {
            parts.add("+" + fx.shards() + " shards");
        }
        if (fx.heal() > 0) {
            parts.add("heal " + fx.heal());
        }
        if (fx.damage() > 0) {
            parts.add("take " + fx.damage() + " damage");
        }
        if (fx.maxHp() > 0) {
            parts.add("+" + fx.maxHp() + " max HP");
        }
        if (fx.strength() > 0) {
            parts.add("+" + fx.strength() + " strength");
        }
        if (fx.draft()) {
            parts.add("draft a card");
        }
        if (fx.relic()) {
            parts.add("gain a relic");
        }
        if (fx.curse()) {
            parts.add("gain a curse");
        }
        if (fx.removeBasic()) {
            parts.add("remove a basic");
        }
        if (fx.companion()) {
            parts.add("recruit a companion");
        }
        return String.join(", ", parts);
    }

    public static String requiresBlurb(EventCost cost) {
        if (cost == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (cost.gold() > 0) {
            parts.add(cost.gold() + "g");
        }
        if (cost.dust() > 0) {
            parts.add(cost.dust() + " dust");
        }
        if (cost.shards() > 0) {
            parts.add(cost.shards() + " shards");
        }
        if (cost.hp() > 0) {
            parts.add("hp>" + cost.hp());
        }
        return parts.isEmpty() ? "" : "needs " + String.join(", ", parts);
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
