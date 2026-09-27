package com.chris.cardgame.run;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Daily-seed playtester feature: maps a UTC calendar date to a
 * deterministic run seed, so every player who sails "today" shares the
 * same campaign. Pure function of the date — stable across runs, JVMs,
 * and time zones. Threading into {@code GameSession} is a follow-up
 * (see lane report): callers pass {@code today()} as the run seed.
 */
public final class DailySeed {
    private DailySeed() {
    }

    /** Deterministic seed for a UTC calendar date. */
    public static long seedFor(LocalDate date) {
        return mix64(date.toEpochDay() * 0x9E3779B97F4A7C15L + 0xBF58476D1CE4E5B9L);
    }

    /** Deterministic seed for the UTC calendar date containing this instant. */
    public static long seedFor(Instant instant) {
        return seedFor(instant.atOffset(ZoneOffset.UTC).toLocalDate());
    }

    /** Seed for today (UTC). */
    public static long today() {
        return seedFor(Instant.now());
    }

    /** Seed for "today" on the given clock (tests). */
    public static long today(Clock clock) {
        return seedFor(Instant.now(clock));
    }

    /** SplitMix64 finalizer: spreads adjacent epoch-days across long space. */
    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
