package com.chris.cardgame.combat;

import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.Row;

public final class DamageCalc {
    public static final double ADVANTAGE = 1.5;
    public static final double DISADVANTAGE = 0.75;
    public static final double COVER = 0.75;
    public static final double WEAK = 0.75;
    public static final double VULNERABLE = 1.25;

    private DamageCalc() {
    }

    public static int attackDamage(Combatant attacker, Combatant target, int base, boolean coverApplies) {
        double mult = 1.0;
        if (attacker.aspect().beats(target.aspect())) {
            mult *= ADVANTAGE;
        } else if (target.aspect().beats(attacker.aspect())) {
            mult *= DISADVANTAGE;
        }
        if (coverApplies && target.row() == Row.BACK) {
