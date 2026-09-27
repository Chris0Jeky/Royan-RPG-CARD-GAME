package com.chris.cardgame.run;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.SplittableRandom;

import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.EnemyDef;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Quick-skirmish backend: escalating difficulty tiers with data-driven
 * enemy pools (see {@code /data/skirmish.json}). Higher tiers field
 * stronger enemies and larger packs; bosses are excluded (skirmish decks
 * are starter decks with no boons, relics, or companions, so boss phases
 * would be a stat wall rather than a fight).
 */
public class Skirmish {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkirmishTier(int tier, String name, List<List<String>> pools) {
    }

    private final List<SkirmishTier> tiers;

    private Skirmish(List<SkirmishTier> tiers) {
        this.tiers = List.copyOf(tiers);
    }

    public static Skirmish load() {
        try (InputStream in = Skirmish.class.getResourceAsStream("/data/skirmish.json")) {
            if (in == null) {
                throw new IllegalStateException("missing /data/skirmish.json on classpath");
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);
            List<SkirmishTier> tiers = new java.util.ArrayList<>();
            for (JsonNode node : root.get("tiers")) {
                tiers.add(mapper.readValue(node.traverse(), SkirmishTier.class));
            }
            return new Skirmish(tiers);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load skirmish.json", e);
        }
    }

    public List<SkirmishTier> tiers() {
        return tiers;
    }

    public int tierCount() {
        return tiers.size();
    }

    public SkirmishTier tier(int tier) {
        return tiers.stream()
                .filter(candidate -> candidate.tier() == tier)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown skirmish tier: " + tier));
    }

    /**
     * Picks one foe pack from the tier's pools. Deterministic in
     * {@code rng}: the same seed yields the same pack.
     */
    public List<EnemyDef> encounterFor(int tier, EnemyLoader enemies, SplittableRandom rng) {
        SkirmishTier def = tier(tier);
        if (def.pools() == null || def.pools().isEmpty()) {
            throw new IllegalArgumentException("empty skirmish pool for tier " + tier);
        }
        return def.pools().get(rng.nextInt(def.pools().size())).stream()
                .map(enemies::get)
                .toList();
    }
}
