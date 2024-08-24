package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.cli.GameLoop;
import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.EnemyLoader;
import com.chris.cardgame.model.CardDef;
import com.chris.cardgame.model.Combatant;
import com.chris.cardgame.model.EnemyDef;
import com.chris.cardgame.model.HeroClass;
import org.junit.jupiter.api.Test;

class BattlePlaythroughTest {
    private final PrintStream silent = new PrintStream(OutputStream.nullOutputStream());

    @Test
    void knightStarterBeatsRatAndImp() {
        CardLoader cards = CardLoader.load();
        EnemyLoader enemies = EnemyLoader.load();
        Combatant hero = Combatant.hero("Captain", HeroClass.KNIGHT, 60);
        List<CardDef> deck = cards.starterDeck(HeroClass.KNIGHT);
        List<EnemyDef> foes = List.of(enemies.get("rat"), enemies.get("imp"));

        GameLoop.BattleResult result =
                new GameLoop().runAutoBattle(hero, deck, foes, 7L, 50, silent);

        assertThat(result.victory()).isTrue();
        assertThat(result.enemiesSlain()).isEqualTo(2);
