package com.chris.cardgame.run;

import java.util.random.RandomGenerator;

/** Snapshotable default-gamma SplitMix stream, matching seeded SplittableRandom draws. */
public final class RunRandom implements RandomGenerator {
    private long state;

    public RunRandom(long state) {
        this.state = state;
    }

    public long state() {
        return state;
    }

    private long advance() {
        return state += 0x9e3779b97f4a7c15L;
    }

    @Override
    public long nextLong() {
        long z = advance();
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    @Override
    public int nextInt() {
        long z = advance();
        z = (z ^ (z >>> 33)) * 0x62a9d9ed799705f5L;
        return (int) (((z ^ (z >>> 28)) * 0xcb24d0a5c88c35b3L) >>> 32);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = nextInt();
        int mask = bound - 1;
        if ((bound & mask) == 0) {
            return r & mask;
        }
        for (int u = r >>> 1; u + mask - (r = u % bound) < 0; u = nextInt() >>> 1) {
            // Rejected draws advance the saved state too.
        }
        return r;
    }

    @Override
    public boolean nextBoolean() {
        return nextInt() < 0;
    }
}
