package com.chris.cardgame.combat;

public record Intent(IntentKind kind, int preview) {

    @Override
    public String toString() {
        return switch (kind) {
            case ATTACK -> "attacks for ~" + preview;
            case DEFEND -> "defends (+" + preview + " block)";
            case BUFF -> "grows stronger (+" + preview + " str)";
            case DEBUFF -> "weakens you";
        };
    }
}
