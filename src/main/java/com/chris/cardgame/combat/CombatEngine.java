package com.chris.cardgame.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.Row;

public class CombatEngine {
    public static final int ENERGY_PER_TURN = 3;
    public static final int DRAW_PER_TURN = 4;
    public static final int ENEMY_DEFEND_BLOCK = 6;
    public static final int ENEMY_BUFF_STRENGTH = 2;

