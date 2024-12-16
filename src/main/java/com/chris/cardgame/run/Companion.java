package com.chris.cardgame.run;

import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.CompanionDef;
import com.chris.cardgame.model.Row;

public class Companion {
    private final CompanionDef def;
    private int hp;

    public Companion(CompanionDef def) {
        this.def = def;
        this.hp = def.hp();
    }

    public CompanionDef def() {
        return def;
    }

    public int hp() {
        return hp;
    }

    public boolean alive() {
        return hp > 0;
