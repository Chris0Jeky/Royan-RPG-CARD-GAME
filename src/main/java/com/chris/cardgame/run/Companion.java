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
    }

    public void rest() {
        hp = Math.min(def.hp(), hp + Math.max(1, def.hp() / 4));
    }

    public Combatant toCombatant() {
        Combatant fighter = new Combatant(def.name(), def.aspect(), Row.BACK, def.hp());
        if (hp < def.hp()) {
            fighter.takeDamage(def.hp() - hp);
        }
        return fighter;
    }

    public void syncFrom(Combatant fighter) {
        hp = fighter.hp();
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, Math.min(def.hp(), hp));
    }
}
