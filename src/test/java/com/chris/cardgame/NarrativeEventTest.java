package com.chris.cardgame;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.chris.cardgame.data.CardLoader;
import com.chris.cardgame.data.CompanionLoader;
import com.chris.cardgame.data.EventLoader;
import com.chris.cardgame.data.RelicLoader;
import com.chris.cardgame.model.EventDef;
import com.chris.cardgame.model.EventDef.EventChoice;
import com.chris.cardgame.model.EventDef.EventCost;
import com.chris.cardgame.model.EventDef.EventEffect;
import com.chris.cardgame.model.HeroClass;
