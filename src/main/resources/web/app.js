"use strict";
/* Royan browser client (W4): campaign + skirmish, art, sound, saga, codex. */

const el = (id) => document.getElementById(id);
const table = el("table");
const stage = el("stage");
const stageBody = el("stage-body");
const logpanel = el("logpanel");
const selectScreen = el("select-screen");
const endScreen = el("end-screen");
const runbar = el("runbar");
const arrivalBox = el("arrival");
const warband = el("warband");

let snap = null;
let prevHp = new Map();
let prevBlock = new Map();
let selected = null;
let busy = false;
let mode = "run";
let knownSave = false;
let shakeTimer = null;
let bannerTimer = null;
let banterTimer = null;
let wbTab = "band";
let codexCache = null;
let codexTab = "heroes";

const NODE_ART = {
  COMBAT: "type-strike", ELITE: "type-power", BOSS: "crown", REST: "heart",
  SHOP: "coin", TAVERN: "tankard", EVENT: "scroll",
};

const NODE_TIPS = {
  COMBAT: "Fight: a standard battle. Victory drafts new cards.",
  ELITE: "Elite: a hard fight with bonus gold, shards, a relic chance, and 2 drafts.",
  BOSS: "Boss: a two-phase captain. Victory clears the act and heals 40%.",
  REST: "Rest: heal 35% of max HP.",
  SHOP: "Shop: buy cards, a relic, and healing with gold.",
  TAVERN: "Tavern: meals, recruits, deck-thinning, and relic trades.",
  EVENT: "Event: a narrative encounter with costed choices.",
};

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

function art(name, cls) {
  return `<img class="art ${cls || ""}" src="/art/${name}.svg" alt="" aria-hidden="true" draggable="false">`;
}

async function post(path, body) {
  const res = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body || {}),
  });
  return res.json();
}

function toast(message) {
  const t = el("toast");
  t.textContent = message;
  t.hidden = false;
  clearTimeout(t._timer);
  t._timer = setTimeout(() => { t.hidden = true; }, 3200);
  if (window.RoyanAudio) RoyanAudio.play("error");
}

function reducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function hpClass(hp, maxHp) {
  const pct = maxHp > 0 ? hp / maxHp : 0;
  if (pct > 0.55) return "high";
  if (pct > 0.25) return "mid";
  return "low";
}

function intentLabel(intent) {
  if (!intent) return "";
  switch (intent.kind) {
    case "ATTACK": return "ATK " + intent.preview;
    case "DEFEND": return "DEFEND";
    case "BUFF": return "EMPOWER";
    case "DEBUFF": return "HEX";
    default: return intent.kind;
  }
}

function intentTip(intent) {
  if (!intent) return "";
  switch (intent.kind) {
    case "ATTACK": return "Attack: this foe strikes for about " + intent.preview + " next turn.";
    case "DEFEND": return "Defend: this foe gains block instead of attacking.";
    case "BUFF": return "Empower: this foe strengthens itself.";
    case "DEBUFF": return "Hex: this foe weakens you (Weak lowers your damage).";
    default: return String(intent.kind).toLowerCase();
  }
}

function aspectTip(aspect) {
  switch (aspect) {
    case "MIGHT": return "Might aspect: beats Guile, loses to Focus (x1.5 / x0.75).";
    case "GUILE": return "Guile aspect: beats Focus, loses to Might (x1.5 / x0.75).";
    case "FOCUS": return "Focus aspect: beats Might, loses to Guile (x1.5 / x0.75).";
    default: return "Aspect: " + aspect;
  }
}

function statusLine(f) {
  const bits = [];
  if (f.strength > 0) bits.push("STR " + f.strength);
  if (f.weak > 0) bits.push("Weak " + f.weak);
  if (f.vulnerable > 0) bits.push("Vuln " + f.vulnerable);
  return bits.map((b) => `<span class="status-chip">${escapeHtml(b)}</span>`).join(" ");
}

function fighterHpHtml(f) {
  const pct = f.maxHp > 0 ? Math.max(0, Math.round((100 * f.hp) / f.maxHp)) : 0;
  return `<div class="bar"><div class="fill ${hpClass(f.hp, f.maxHp)}" style="width:${pct}%"></div></div>
    <div class="hp-line"><span>${f.hp} / ${f.maxHp}</span>
    ${f.block > 0 ? `<span class="block-chip">BLK ${f.block}</span>` : ""}
    ${statusLine(f)}</div>`;
}

function cardHtml(c, cls, attr) {
  return `<div class="card aspect-${c.aspect} ${cls || ""}" ${attr || ""}>
      <div class="card-top"><span class="cost">${c.cost}</span>
        <span class="card-name">${escapeHtml(c.name)}</span>
        ${art("type-" + c.type.toLowerCase(), "cardart")}</div>
      <div class="card-kind">${escapeHtml(c.type)} &middot; ${escapeHtml(c.aspect)}</div>
      <div class="card-text">${escapeHtml(c.text)}</div>
      ${c.flavor ? `<div class="card-flavor">${escapeHtml(c.flavor)}</div>` : ""}
    </div>`;
}

function requiresBlurb(req) {
  const parts = [];
  if (req.gold > 0) parts.push(req.gold + "g");
  if (req.dust > 0) parts.push(req.dust + " dust");
  if (req.shards > 0) parts.push(req.shards + " shards");
  if (req.hp > 0) parts.push("hp>" + req.hp);
  return parts.length ? "needs " + parts.join(", ") : "";
}

/* ---------- battle renderers (shared by skirmish + campaign) ---------- */

function renderFoes(s) {
  const host = el("foes");
  host.innerHTML = s.enemies.map((f) => {
    const targetable = selected !== null && f.alive ? " targetable" : "";
    return `<div class="foe ${f.row === "BACK" ? "back-row" : ""} ${f.alive ? "" : "dead"}${targetable}"
        data-foe="${f.index}" data-hpkey="foe:${f.index}">
      <div class="foe-top">${art(f.foeId || "skull", `portrait aspect-${f.aspect}${f.boss ? " big" : ""}`)}
      <div class="foe-head"><span class="foe-name">${escapeHtml(f.name)}</span>
        <span class="foe-badges">
          <span class="badge aspect-${f.aspect}" title="${escapeHtml(aspectTip(f.aspect))}">${escapeHtml(f.aspect)}</span>
          <span class="badge row-badge" title="${f.row === "BACK" ? "Back row: takes less damage while any front-row foe stands." : "Front row: kill these first to expose the back row."}">${f.row === "BACK" ? "BACK" : "FRONT"}</span>
        </span></div></div>
      ${fighterHpHtml(f)}
      ${f.alive ? `<span class="intent intent-${f.intent.kind}" title="${escapeHtml(intentTip(f.intent))}">${escapeHtml(intentLabel(f.intent))}</span>` : `<span class="intent" title="This foe is down.">DOWN</span>`}
      ${f.flavor ? `<div class="foe-flavor">${escapeHtml(f.flavor)}</div>` : ""}
    </div>`;
  }).join("");
}

function allyArt(s, index) {
  if (s.run && s.run.companions && s.run.companions[index]) {
    return "role-" + s.run.companions[index].role.toLowerCase();
  }
  return "sail";
}

function renderAllies(s) {
  const host = el("allies");
  if (!s.companions || !s.companions.length) {
    host.innerHTML = "";
    return;
  }
  host.innerHTML = s.companions.map((a) => `
    <div class="ally" data-hpkey="ally:${a.index}">
      <div class="foe-top">${art(allyArt(s, a.index), "portrait")}
      <span class="aname">${escapeHtml(a.name)}</span></div>
      ${fighterHpHtml(a)}
    </div>`).join("");
}

function heroClassOf(s) {
  if (s.heroClass) return s.heroClass;
  if (s.run) return s.run.heroClass;
  return "KNIGHT";
}

function renderHero(s) {
  const h = s.hero;
  const cls = heroClassOf(s);
  el("hero").innerHTML = `
    <div class="hero-top">${art("hero-" + cls.toLowerCase(), `portrait big aspect-${h.aspect}`)}
    <div class="hero-title">
    <div class="foe-head"><span class="hero-name">${escapeHtml(h.name)}</span>
      <span class="foe-badges"><span class="badge aspect-${h.aspect}" title="${escapeHtml(aspectTip(h.aspect))}">${escapeHtml(h.aspect)}</span></span></div>
    <div class="hero-sub">${escapeHtml(titleCase(cls))} &middot; Turn ${s.turn}</div>
    </div></div>
    <div data-hpkey="hero">${fighterHpHtml(h)}</div>`;
  el("turnbox").innerHTML = `
    <div class="energy" title="Energy refills to 3 each turn; cards cost energy to play.">${art("energy", "")}${s.energy} energy</div>
    <div class="piles" title="You draw 4 cards each turn; the draw pile reshuffles from discard when empty.">Draw ${s.drawCount} &middot; Discard ${s.discardCount}</div>
    <button id="btn-end" class="btn brass" type="button" ${s.over ? "disabled" : ""}>End turn</button>`;
  el("btn-end").addEventListener("click", onEndTurn);
}

function renderHand(s) {
  el("hand").innerHTML = s.hand.map((c) =>
    cardHtml(c, `${c.playable ? "" : "unplayable"}${selected === c.index ? " selected" : ""}`,
      `data-card="${c.index}"`)).join("");
}

function renderLog(s) {
  const log = el("log");
  log.innerHTML = s.log.map((line) => `<div>${escapeHtml(line)}</div>`).join("");
  log.scrollTop = log.scrollHeight;
}

/* ---------- campaign chrome ---------- */

function renderRunbar(s) {
  const r = s.run;
  const pct = r.xpNext > 0 ? Math.min(100, Math.round((100 * r.xp) / r.xpNext)) : 0;
  runbar.innerHTML = `
    <span><strong>Act ${r.act}</strong> &middot; Lv ${r.level}</span>
    <span class="xpbar" title="${r.xp} / ${r.xpNext} XP"><div style="width:${pct}%"></div></span>
    <span class="coin" title="Gold: spend in shops and taverns on cards, relics, healing, and recruits.">${art("coin", "")}${r.gold}g</span>
    <span title="Dust: strike basic cards from your deck at taverns and forges; skipped drafts salvage to dust.">${art("dust", "")}${r.dust} dust</span>
    <span title="Shards: elite and boss loot for back-room relic trades.">${art("shard", "")}${r.shards} shards</span>
    <button class="linklike" id="btn-warband" type="button">War-band (${r.deckSize} cards, ${r.relics.length} relics, ${r.companions.length} allies)</button>
    <span class="spacer"></span>
    ${s.screen === "map" && !s.over ? `<button class="btn ghost" id="btn-abandon" type="button">Suspend</button>` : ""}`;
  runbar.hidden = false;
  el("btn-warband").addEventListener("click", renderWarband);
  const abandon = el("btn-abandon");
  if (abandon) abandon.addEventListener("click", onAbandon);
}

function renderArrival(s) {
  if (s.arrival) {
    arrivalBox.innerHTML = `<h3>${escapeHtml(s.arrival.title)}</h3><p>${escapeHtml(s.arrival.text)}</p>`;
    arrivalBox.hidden = false;
  } else {
    arrivalBox.hidden = true;
  }
}

function renderBanter(s) {
  const box = el("banter");
  clearTimeout(banterTimer);
  if (!s.banter) {
    box.hidden = true;
    return;
  }
  const roleArt = "role-" + String(s.banter.role).toLowerCase();
  box.innerHTML = `<div class="bhead">${art(roleArt, "")}<div><div class="speaker">${escapeHtml(s.banter.speaker)}</div><div class="brole">${escapeHtml(s.banter.role)}</div></div></div><div class="btext">&ldquo;${escapeHtml(s.banter.text)}&rdquo;</div>`;
  box.hidden = false;
  banterTimer = setTimeout(() => { box.hidden = true; }, 6000);
}

function bandHtml() {
  const r = snap.run;
  return `
    <h3>Deck (${r.deckSize})</h3>
    <ul>${r.deck.map((c) => `<li>${art("type-" + c.type.toLowerCase(), "")}${c.cost} — ${escapeHtml(c.name)} <em>${escapeHtml(c.type)}</em></li>`).join("")}</ul>
    <h3>Relics (${r.relics.length})</h3>
    <ul>${r.relics.length ? r.relics.map((x) => `<li>${art("relic", "")}<strong>${escapeHtml(x.name)}</strong> (${escapeHtml(x.effect)} ${x.value})<br><em>${escapeHtml(x.flavor || "")}</em></li>`).join("") : "<li><em>No relics yet.</em></li>"}</ul>
    <h3>Companions (${r.companions.length})</h3>
    <ul>${r.companions.length ? r.companions.map((c) => `<li>${art("role-" + c.role.toLowerCase(), "")}<strong>${escapeHtml(c.name)}</strong> — ${escapeHtml(c.role)} (${c.hp}/${c.maxHp})</li>`).join("") : "<li><em>None yet. Taverns hire blades.</em></li>"}</ul>`;
}

function sagaHtml() {
  const entries = snap.chronicle || [];
  if (!entries.length) return "<p><em>The saga has yet to be written.</em></p>";
  return `<div class="chron">` + entries.map((e) =>
    `<div class="chron-entry"><span class="chron-act">A${e.act}</span><span class="chron-kind k-${e.kind}">${escapeHtml(e.kind)}</span><span>${escapeHtml(e.text)}</span></div>`
  ).join("") + `</div>`;
}

function renderWarband() {
  warband.innerHTML = `
    <button class="btn ghost closebtn" id="btn-closewb" type="button">Close</button>
    <h2>War-band</h2>
    <div class="wbtabs">
      <button class="modebtn ${wbTab === "band" ? "on" : ""}" type="button" data-wtab="band">War-band</button>
      <button class="modebtn ${wbTab === "saga" ? "on" : ""}" type="button" data-wtab="saga">Saga</button>
    </div>
    ${wbTab === "band" ? bandHtml() : sagaHtml()}`;
  warband.hidden = false;
  el("btn-closewb").addEventListener("click", () => { warband.hidden = true; });
  warband.querySelectorAll("[data-wtab]").forEach((btn) => {
    btn.addEventListener("click", () => { wbTab = btn.dataset.wtab; renderWarband(); });
  });
}

/* ---------- stage screens ---------- */

function stageMap(s) {
  const m = s.map;
  const options = new Set(m.options);
  const byLayer = new Map();
  m.nodes.forEach((n) => {
    if (!byLayer.has(n.layer)) byLayer.set(n.layer, []);
    byLayer.get(n.layer).push(n);
  });
  let pickNo = 0;
  const cols = [...byLayer.entries()].sort((a, b) => a[0] - b[0]).map(([layer, nodes]) => `
    <div class="maplayer"><span class="layertitle">LAYER ${layer + 1}</span>
    ${nodes.map((n) => {
      const isOpt = options.has(n.id);
      const isHere = m.currentId === n.id;
      const cls = `node${isOpt ? " opt" : ""}${isHere ? " here" : ""}`;
      const glyph = art(NODE_ART[n.type] || "sail", "");
      if (!isOpt) {
        return `<div class="${cls}" title="${escapeHtml(NODE_TIPS[n.type] || n.type)}"><span class="ntype ntype-${n.type}">${glyph}${n.type}</span><span class="nid">${escapeHtml(n.id)}${isHere ? " — here" : ""}</span></div>`;
      }
      pickNo++;
      return `<button class="${cls}" type="button" data-node="${escapeHtml(n.id)}" data-pick="${pickNo}" title="${escapeHtml(NODE_TIPS[n.type] || n.type)}"><span class="ntype ntype-${n.type}">${glyph}<span class="picknum">${pickNo}</span>${n.type}</span><span class="nid">${escapeHtml(n.id)}</span></button>`;
    }).join("")}</div>`).join("");
  return `<h2>Act ${s.run.act} — Chart your course</h2>
    <p class="lede">${m.currentId ? "Choose the next isle. The boss waits at the sky's end." : "Choose your landing isle."}</p>
    <div class="mapcols">${cols}</div>`;
}

function stageLevelup(s) {
  const l = s.levelup;
  return `<h2>Level ${l.level}!</h2>
    <p class="lede">Choose a boon.${l.pending > 1 ? ` (${l.pending} await.)` : ""}</p>
    <div class="boons">${l.offer.map((b, i) => `
      <button class="boon" type="button" data-boon="${b.index}" data-pick="${i + 1}">
        <span class="picknum">${i + 1}</span><h3>${escapeHtml(b.name)}</h3><p>${escapeHtml(b.desc)}</p>
      </button>`).join("")}</div>`;
}

function stageDraft(s) {
  const d = s.draft;
  return `<h2>Draft a card</h2>
    <p class="lede">Spoils of victory — take one, or salvage ${d.salvage} dust. (${d.left} left.)</p>
    <div class="drafts">${d.options.map((c, i) =>
      `<div class="shopitem"><span class="picknum">${i + 1}</span>${cardHtml(c, "", `data-draft="${c.index}" data-pick="${i + 1}"`)}</div>`).join("")}</div>
    <div class="draft-actions"><button class="btn ghost" type="button" data-skip="1">Salvage instead (+${d.salvage} dust)</button></div>`;
}

function stageShop(s) {
  const sh = s.shop;
  const stock = sh.stock.length ? sh.stock.map((e, i) => `
      <div class="shopitem">${cardHtml(e.card, "", "")}
        <button class="buybtn" type="button" data-buycard="${e.card.index}" data-pick="${i + 1}"><span class="picknum">${i + 1}</span>Buy — ${e.price}g</button>
      </div>`).join("") : `<p class="lede">Sold out.</p>`;
  const relic = sh.relic ? `
      <div class="sidecard"><h3>${art("relic", "sideart")}${escapeHtml(sh.relic.relic.name)}</h3>
        <p>${escapeHtml(sh.relic.relic.effect)} ${sh.relic.relic.value} &middot; ${escapeHtml(sh.relic.relic.rarity)}<br><em>${escapeHtml(sh.relic.relic.flavor || "")}</em></p>
        <button class="buybtn" type="button" data-buyrelic="1">Buy relic — ${sh.relic.price}g</button>
      </div>`
    : `<div class="sidecard"><h3>Relic vault</h3><p>Nothing left but dust.</p></div>`;
  return `<h2>Sky-dock shop</h2>
    <p class="lede">Purse: <strong>${s.run.gold}g</strong>. The keep polishes glass and waits.</p>
    <div class="shopgrid">${stock}</div>
    <div class="shopside">${relic}
      <div class="sidecard"><h3>${art("heart", "sideart")}Patch up</h3><p>Healers close your wounds: +${sh.healAmount} HP.</p>
        <button class="buybtn" type="button" data-buyheal="1">Heal — ${sh.healCost}g</button></div>
      <div class="sidecard"><h3>Weigh anchor</h3><p>Back to the open sky.</p>
        <button class="btn ghost" type="button" data-leave="shop">Leave shop</button></div>
    </div>`;
}

function stageTavern(s) {
  const t = s.tavern;
  const r = s.run;
  const card = (action, icon, title, desc) => `
    <div class="sidecard"><h3>${art(icon, "sideart")}${title}</h3><p>${desc}</p>
      <button class="buybtn" type="button" data-tavern="${action}">Choose</button></div>`;
  return `<h2>The Gilded Gale</h2>
    <p class="lede">Purse: <strong>${r.gold}g</strong>, ${r.dust} dust, ${r.shards} shards. War-band: ${r.companions.length}/${r.maxCompanions}.</p>
    <div class="taverngrid">
      ${card("meal", "heart", "Hearty meal", `+50% HP for <span class="price">${t.mealCost}g</span>.`)}
      ${card("recruit", "role-striker", "Hire a blade", `A companion for <span class="price">${t.recruitCost}g</span>.`)}
      ${card("remove", "type-curse", "Strike a basic", `Thin the deck for <span class="price">${t.removeCost} dust</span>.`)}
      ${card("relic", "relic", "Back-room trade", `A relic for <span class="price">${t.relicGold}g + ${t.relicShards} shard</span>.`)}
    </div>
    <div class="draft-actions"><button class="btn ghost" type="button" data-leave="tavern">Leave tavern</button></div>`;
}

function stageEvent(s) {
  const e = s.event;
  let n = 0;
  return `<h2>${art("scroll", "sideart")}${escapeHtml(e.title)}</h2>
    <div class="eventtext">${escapeHtml(e.text)}</div>
    <div class="choices">${e.choices.map((c) => {
      const meta = [c.requires ? requiresBlurb(c.requires) : "", c.effects].filter(Boolean).join(" — ");
      if (!c.affordable) {
        return `<button class="choice poor" type="button" disabled><span class="ctext">${escapeHtml(c.text)}</span>${meta ? `<span class="cmeta">${escapeHtml(meta)}</span>` : ""}</button>`;
      }
      n++;
      return `<button class="choice" type="button" data-choice="${c.index}" data-pick="${n}"><span class="ctext"><span class="picknum">${n}</span>${escapeHtml(c.text)}</span>${meta ? `<span class="cmeta">${escapeHtml(meta)}</span>` : ""}</button>`;
    }).join("")}</div>`;
}

function renderStage(s) {
  const builders = {
    map: stageMap, levelup: stageLevelup, draft: stageDraft,
    shop: stageShop, tavern: stageTavern, event: stageEvent,
  };
  stageBody.innerHTML = builders[s.screen](s);
  stage.hidden = false;
}

const GUIDE = [
  { term: "Energy", text: "You gain 3 energy each turn; every card costs energy to play. Unspent energy is lost." },
  { term: "Draw and discard", text: "Draw 4 cards each turn. Your hand discards at end of turn; an empty draw pile reshuffles from discard." },
  { term: "Card types", text: "Strikes deal damage; Guards grant block; Tricks mix damage, debuffs and utility; Powers buff you for the fight; Curses are unplayable and clog your hand." },
  { term: "Intents", text: "Foes telegraph their next move: ATK with incoming damage, DEFEND for block, EMPOWER for buffs, HEX to weaken you." },
  { term: "Aspects", text: "Might beats Guile beats Focus beats Might. Advantage deals x1.5, disadvantage x0.75. Your card aspect counts on attacks." },
  { term: "Rows", text: "Back-row foes take x0.75 damage while any front-row foe stands. Kill the front first." },
  { term: "Block", text: "Block absorbs damage before HP. Your block clears at the start of your next turn." },
  { term: "Weak and Vulnerable", text: "Weak fighters deal x0.75 damage. Vulnerable fighters take x1.25 damage." },
  { term: "Strength", text: "Each point of Strength adds to attack damage." },
  { term: "Bosses", text: "Bosses transform at 50% HP: new attack, new behavior, new aspect, plus healing. Read the herald line." },
  { term: "Gold, dust, shards", text: "Gold buys cards, relics, healing and recruits. Dust strikes basic cards from your deck. Shards buy back-room relics." },
  { term: "Drafts and boons", text: "After victories take 1 of 3 cards (2 from elites and bosses) or salvage +10 dust. Each level offers 1 of 3 boons." },
  { term: "Companions", text: "Up to 2 hired blades fight beside you and act before foes: Strikers hit, Guardians block for you, Medics heal the most-hurt ally." },
];

/* ---------- codex ---------- */

async function openCodex() {
  el("codex-screen").hidden = false;
  if (codexTab === "guide") {
    renderCodex();
    return;
  }
  if (!codexCache) {
    el("codex-body").innerHTML = "<p>Unfurling scrolls…</p>";
    try {
      codexCache = await (await fetch("/api/codex")).json();
    } catch (e) {
      el("codex-body").innerHTML = "<p>Could not reach the archives.</p>";
      return;
    }
  }
  renderCodex();
}

function closeCodex() {
  el("codex-screen").hidden = true;
}

function renderCodex() {
  document.querySelectorAll("#codex-tabs [data-ctab]").forEach((b) => {
    b.classList.toggle("on", b.dataset.ctab === codexTab);
  });
  el("codex-filter").hidden = codexTab !== "cards";
  const body = el("codex-body");
  if (codexTab === "guide") {
    body.innerHTML = GUIDE.map((g) => `
      <div class="guide-entry"><span class="guide-term">${escapeHtml(g.term)}</span><p>${escapeHtml(g.text)}</p></div>`).join("");
    return;
  }
  if (codexTab === "heroes") {
    body.innerHTML = codexCache.heroes.map((h) => `
      <div class="codex-hero"><h3>${art("hero-" + h.id.toLowerCase(), "")}${escapeHtml(h.name)} — ${escapeHtml(h.title)}</h3>
        <p><strong>${h.hp} HP &middot; ${escapeHtml(h.aspect)}</strong></p>
        <p>${escapeHtml(h.origin)}</p>
        <p><em>${escapeHtml(h.motive)}</em></p></div>`).join("");
  } else if (codexTab === "foes") {
    body.innerHTML = `<div class="codex-grid">` + codexCache.enemies.map((f) => `
      <div class="codex-foe${f.boss ? " boss" : ""}"><h4>${art(f.id, "")}${escapeHtml(f.name)}</h4>
        <div class="stats">${f.hp} HP &middot; ${f.atk} ATK &middot; ${escapeHtml(f.aspect)}</div>
        <p>${escapeHtml(f.behavior)} &middot; ${escapeHtml(f.row)} &middot; ${f.xp} XP${f.boss ? " &middot; BOSS" : ""}</p>
        <p><em>${escapeHtml(f.flavor || "")}</em></p></div>`).join("") + `</div>`;
  } else if (codexTab === "cards") {
    const want = el("codex-class").value;
    const cards = codexCache.cards.filter((c) => !want || c.heroClass === want);
    body.innerHTML = `<div class="codex-grid">` + cards.map((c) => cardHtml(c, "", "")).join("") + `</div>`;
  } else if (codexTab === "relics") {
    body.innerHTML = `<div class="codex-grid">` + codexCache.relics.map((x) => `
      <div class="codex-foe"><h4>${art("relic", "")}${escapeHtml(x.name)}</h4>
        <div class="stats">${escapeHtml(x.effect)} ${x.value} &middot; ${escapeHtml(x.rarity)}</div>
        <p><em>${escapeHtml(x.flavor || "")}</em></p></div>`).join("") + `</div>`;
  } else {
    body.innerHTML = `<div class="codex-grid">` + codexCache.companions.map((c) => `
      <div class="codex-foe"><h4>${art("role-" + c.role.toLowerCase(), "")}${escapeHtml(c.name)}</h4>
        <div class="stats">${escapeHtml(c.role)} &middot; ${c.hp} HP &middot; power ${c.power}</div>
        <p><em>${escapeHtml(c.flavor || "")}</em></p></div>`).join("") + `</div>`;
  }
}

const TUT_HIDE_KEY = "royan-hide-tutorial";

function openTutorial() {
  el("tutorial-hide").checked = false;
  el("tutorial-screen").hidden = false;
}

function closeTutorial() {
  try {
    if (el("tutorial-hide").checked) localStorage.setItem(TUT_HIDE_KEY, "1");
  } catch (e) {
    /* private mode: show again next visit */
  }
  el("tutorial-screen").hidden = true;
}

function maybeShowTutorial() {
  let hide = false;
  try {
    hide = localStorage.getItem(TUT_HIDE_KEY) === "1";
  } catch (e) {
    hide = false;
  }
  if (!hide) openTutorial();
}

/* ---------- select + chrome ---------- */

function renderSelect(s) {
  knownSave = !!s.hasSave;
  const saveErr = el("save-error");
  if (s.canStartFresh && s.error) {
    saveErr.innerHTML = `<span>${escapeHtml(s.error)}</span><button class="btn ghost" id="btn-saveerr-ok" type="button">Understood</button>`;
    saveErr.hidden = false;
    el("btn-saveerr-ok").addEventListener("click", () => { saveErr.hidden = true; });
  } else {
    saveErr.hidden = true;
  }
  const host = el("heroes");
  host.innerHTML = s.heroes.map((h) => `
    <button class="hero-pick" type="button" data-hero="${h.id}">
      ${art("hero-" + h.id.toLowerCase(), "pickart")}
      <h2>${escapeHtml(h.name)}</h2>
      <div class="hero-title-line">${escapeHtml(h.title || "")}</div>
      <span class="hp">${h.hp} HP &middot; ${escapeHtml(h.aspect)}</span>
      <p class="origin">${escapeHtml(h.origin || "")}</p>
      <p class="motive">&ldquo;${escapeHtml(h.motive || "")}&rdquo;</p>
      <p>${escapeHtml(h.blurb)}</p>
      <span class="sail">Set sail &rarr;</span>
    </button>`).join("");
  host.querySelectorAll("[data-hero]").forEach((btn) => {
    btn.addEventListener("click", () => onSail(btn.dataset.hero));
  });
  el("continue-row").hidden = !knownSave;
}

function titleCase(word) {
  const lower = String(word).toLowerCase();
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}

function spawnFloater(panel, text, cls) {
  const span = document.createElement("span");
  span.className = "floater" + (cls ? " " + cls : "");
  span.textContent = text;
  panel.appendChild(span);
  setTimeout(() => span.remove(), 1200);
}

function showFloaters(s) {
  const reduce = reducedMotion();
  const check = (key, f) => {
    const prev = prevHp.get(key);
    const prevB = prevBlock.get(key) || 0;
    prevHp.set(key, f.hp);
    prevBlock.set(key, f.block);
    if (reduce || prev === undefined) return;
    const host = document.querySelector(`[data-hpkey="${key}"]`);
    if (!host) return;
    const panel = host.closest(".foe, .hero, .ally");
    const diff = f.hp - prev;
    if (diff !== 0) {
      spawnFloater(panel, (diff > 0 ? "+" : "") + diff,
        diff > 0 ? "heal" : (diff <= -10 ? "big" : ""));
      panel.classList.add("hitflash");
      setTimeout(() => panel.classList.remove("hitflash"), 350);
      if (prev > 0 && f.hp <= 0) panel.classList.add("dying");
    }
    if (f.block > prevB) spawnFloater(panel, "+" + (f.block - prevB) + " BLK", "blk");
  };
  check("hero", s.hero);
  s.enemies.forEach((f) => check("foe:" + f.index, f));
  (s.companions || []).forEach((a) => check("ally:" + a.index, a));
}

function shake(size) {
  if (reducedMotion()) return;
  document.body.classList.remove("shake-sm", "shake-md");
  void document.body.offsetWidth;
  document.body.classList.add(size === "md" ? "shake-md" : "shake-sm");
  clearTimeout(shakeTimer);
  shakeTimer = setTimeout(() => {
    document.body.classList.remove("shake-sm", "shake-md");
  }, 400);
}

function turnBanner(n) {
  if (reducedMotion()) return;
  const b = el("turnbanner");
  b.textContent = "Turn " + n;
  b.hidden = false;
  b.style.animation = "none";
  void b.offsetWidth;
  b.style.animation = "";
  clearTimeout(bannerTimer);
  bannerTimer = setTimeout(() => { b.hidden = true; }, 1450);
}

function sfx(prev, s) {
  const A = window.RoyanAudio;
  if (!A || !prev || prev.phase === "select") return;
  let hit = false;
  let bigFoe = false;
  if (s.enemies && prev.enemies) {
    for (const f of s.enemies) {
      const p = prev.enemies.find((x) => x.index === f.index);
      if (p && f.hp < p.hp) {
        hit = true;
        if (p.hp - f.hp >= 12) bigFoe = true;
      }
    }
  }
  if (s.hero && prev.hero && s.hero.hp < prev.hero.hp) {
    hit = true;
    shake(prev.hero.hp - s.hero.hp >= 8 ? "md" : "sm");
  } else if (bigFoe) {
    shake("sm");
  }
  if (hit) A.play("hit");
  if (s.hand && prev.hand && s.hand.length < prev.hand.length) A.play("card");
  if (s.hero && prev.hero && s.hero.block > prev.hero.block) A.play("block");
  if (s.hero && prev.hero && s.hero.hp > prev.hero.hp) A.play("heal");
  if (s.turn && prev.turn && s.turn > prev.turn && !s.over) {
    A.play("turn");
    turnBanner(s.turn);
  }
  if (s.over && !prev.over) A.play(s.victory ? "victory" : "defeat");
  if (s.mode === "run" && prev.mode === "run" && s.run && prev.run) {
    if (s.run.gold > prev.run.gold) A.play("coin");
    if (s.screen === "levelup" && prev.screen !== "levelup") A.play("boon");
    if (s.screen === "draft" && prev.screen !== "draft") A.play("draft");
  }
}

function updateChrome(s) {
  el("battle-label").textContent = s.label || "Sky-Isles";
  const chip = el("seed-chip");
  if (s.seed !== undefined) {
    chip.textContent = "seed " + s.seed;
    chip.hidden = false;
  } else {
    chip.hidden = true;
  }
}

function showEnd(s) {
  const r = s.run;
  let icon = "skull";
  let eyebrow = "";
  let title = "";
  let main = "";
  let quote = "";
  if (s.mode === "run" && s.abandoned) {
    icon = "sail";
    eyebrow = "Charts held by the Guild";
    title = "Campaign Suspended";
    main = `Act ${r.act}, level ${r.level}. Resume any time with Continue.`;
  } else if (s.mode === "run" && s.victory) {
    icon = "crown";
    eyebrow = "The sky-lanes are yours";
    title = "Campaign Victory";
    main = `${r.heroName}, level ${r.level}: ${r.deckSize} cards, ${r.relics.length} relics, ${r.companions.length} sworn blades. Sung for a hundred years.`;
    quote = r.story ? r.story.triumph : "";
  } else if (s.mode === "run") {
    eyebrow = "The sea claims another";
    title = "Defeat";
    main = `${r.heroName} fell in Act ${r.act} at level ${r.level}. The Guild funds the next voyage.`;
    quote = r.story ? r.story.epitaph : "";
  } else if (s.victory) {
    icon = "crown";
    eyebrow = "The sky-lane is yours";
    title = "Victory";
    main = `The Guild toasts ${s.heroName}: ${s.enemies.length} foes broken in ${s.turn} turns. Try the full campaign from the menu.`;
  } else {
    eyebrow = "The sea claims another";
    title = "Defeat";
    main = `${s.heroName} fell on turn ${s.turn}. The Guild will sing of this voyage — and fund the next one.`;
  }
  el("end-eyebrow").textContent = eyebrow;
  el("end-title").textContent = title;
  el("end-title").insertAdjacentHTML("afterbegin", art(icon, "endart") + " ");
  el("end-text").innerHTML = escapeHtml(main)
    + (quote ? `<span class="endstory">&ldquo;${escapeHtml(quote)}&rdquo;</span>` : "");
  endScreen.hidden = false;
}

function render(s) {
  const old = snap;
  snap = s;
  if (s.error && !s.canStartFresh) toast(s.error);
  warband.hidden = true;
  if (s.phase === "select") {
    table.hidden = true;
    stage.hidden = true;
    logpanel.hidden = true;
    runbar.hidden = true;
    arrivalBox.hidden = true;
    endScreen.hidden = true;
    el("banter").hidden = true;
    selectScreen.hidden = false;
    renderSelect(s);
    updateChrome(s);
    return;
  }
  selectScreen.hidden = true;
  logpanel.hidden = false;
  renderLog(s);
  updateChrome(s);
  sfx(old, s);
  renderBanter(s);
  if (s.mode === "run") {
    renderRunbar(s);
    renderArrival(s);
  } else {
    runbar.hidden = true;
    arrivalBox.hidden = true;
  }
  if (s.mode === "run" && s.screen !== "battle") {
    table.hidden = true;
    renderStage(s);
  } else {
    stage.hidden = true;
    table.hidden = false;
    if (s.over) selected = null;
    renderFoes(s);
    renderAllies(s);
    renderHero(s);
    renderHand(s);
    if (!s.over) showFloaters(s);
    updateTargetBanner();
  }
  stageBody.inert = !!s.over;
  table.inert = !!s.over && s.mode === "run";
  if (s.over) {
    if (s.abandoned) knownSave = true;
    if (s.mode === "run" && !s.abandoned) knownSave = false;
    showEnd(s);
  } else {
    endScreen.hidden = true;
  }
}

function updateTargetBanner() {
  const banner = el("target-banner");
  if (selected === null || !snap || snap.over) {
    banner.hidden = true;
    return;
  }
  const card = snap.hand.find((c) => c.index === selected);
  if (!card || !card.needsTarget) {
    banner.hidden = true;
    return;
  }
  banner.hidden = false;
  banner.textContent = `Choose a foe for ${card.name} — or press Esc to sheathe it.`;
}

/* ---------- actions ---------- */

async function guarded(work) {
  if (busy) return;
  busy = true;
  try {
    await work();
  } catch (e) {
    toast("Could not reach the helm: " + e.message);
  } finally {
    busy = false;
  }
}

function seedBody(heroId) {
  const raw = el("seed-input").value.trim();
  const body = { heroClass: heroId };
  if (raw !== "") {
    const seed = Number(raw);
    if (Number.isFinite(seed)) body.seed = Math.trunc(seed);
  }
  return body;
}

async function onSail(heroId) {
  return guarded(async () => {
    selected = null;
    prevHp.clear();
    prevBlock.clear();
    const path = mode === "run" ? "/api/new-run" : "/api/new-battle";
    render(await post(path, seedBody(heroId)));
  });
}

async function onContinue() {
  return guarded(async () => {
    selected = null;
    prevHp.clear();
    prevBlock.clear();
    const s = await post("/api/continue", {});
    if (!s.error) knownSave = true;
    render(s);
  });
}

async function onAbandon() {
  if (!window.confirm("Suspend this campaign? The Guild will hold your charts.")) return;
  return guarded(async () => {
    selected = null;
    render(await post("/api/abandon", {}));
  });
}

async function onCard(index) {
  if (busy || !snap || snap.over) return;
  const inBattle = snap.mode === "skirmish" ? snap.phase === "battle" : snap.screen === "battle";
  if (!inBattle) return;
  const card = snap.hand.find((c) => c.index === index);
  if (!card || !card.playable) return;
  if (card.needsTarget) {
    selected = selected === index ? null : index;
    renderHand(snap);
    renderFoes(snap);
    updateTargetBanner();
    return;
  }
  return guarded(async () => {
    selected = null;
    render(await post("/api/play", { hand: index, target: -1 }));
  });
}

async function onFoe(index) {
  if (busy || selected === null || !snap || snap.over) return;
  const foe = snap.enemies.find((f) => f.index === index);
  if (!foe || !foe.alive) {
    toast("That foe is already down.");
    return;
  }
  return guarded(async () => {
    const hand = selected;
    selected = null;
    render(await post("/api/play", { hand, target: index }));
  });
}

async function onEndTurn() {
  if (busy || !snap || snap.over) return;
  return guarded(async () => {
    selected = null;
    render(await post("/api/end-turn", {}));
  });
}

async function onChooseNode(id) {
  return guarded(async () => {
    prevHp.clear();
    prevBlock.clear();
    render(await post("/api/choose-node", { id }));
  });
}

async function onChooseBoon(index) {
  return guarded(async () => {
    render(await post("/api/choose-boon", { index }));
  });
}

async function onChooseDraft(index, skip) {
  return guarded(async () => {
    render(await post("/api/choose-draft", skip ? { skip: true } : { index }));
  });
}

async function onShopBuy(kind, index) {
  return guarded(async () => {
    const body = { kind };
    if (index !== null && index !== undefined) body.index = index;
    render(await post("/api/shop-buy", body));
  });
}

async function onLeave(which) {
  return guarded(async () => {
    render(await post(which === "shop" ? "/api/shop-leave" : "/api/tavern-leave", {}));
  });
}

async function onTavern(action) {
  return guarded(async () => {
    render(await post("/api/tavern", { action }));
  });
}

async function onEventChoose(index) {
  return guarded(async () => {
    render(await post("/api/event-choose", { index }));
  });
}

function onNewBattle() {
  selected = null;
  endScreen.hidden = true;
  el("continue-row").hidden = !knownSave;
  selectScreen.hidden = false;
}

function syncMute() {
  const btn = el("btn-mute");
  const muted = window.RoyanAudio ? RoyanAudio.isMuted() : false;
  btn.textContent = muted ? "Sound: off" : "Sound: on";
}

function unlockAudio() {
  if (window.RoyanAudio) RoyanAudio.unlock();
}

/* ---------- wiring ---------- */

el("hand").addEventListener("click", (e) => {
  const card = e.target.closest("[data-card]");
  if (card) onCard(Number(card.dataset.card));
});
el("foes").addEventListener("click", (e) => {
  const foe = e.target.closest("[data-foe]");
  if (foe && selected !== null) onFoe(Number(foe.dataset.foe));
});
stageBody.addEventListener("click", (e) => {
  const t = e.target;
  const node = t.closest("[data-node]");
  if (node) return onChooseNode(node.dataset.node);
  const boon = t.closest("[data-boon]");
  if (boon) return onChooseBoon(Number(boon.dataset.boon));
  const draft = t.closest("[data-draft]");
  if (draft) return onChooseDraft(Number(draft.dataset.draft), false);
  if (t.closest("[data-skip]")) return onChooseDraft(null, true);
  const buy = t.closest("[data-buycard]");
  if (buy) return onShopBuy("card", Number(buy.dataset.buycard));
  if (t.closest("[data-buyrelic]")) return onShopBuy("relic", null);
  if (t.closest("[data-buyheal]")) return onShopBuy("heal", null);
  const leave = t.closest("[data-leave]");
  if (leave) return onLeave(leave.dataset.leave);
  const tav = t.closest("[data-tavern]");
  if (tav) return onTavern(tav.dataset.tavern);
  const choice = t.closest("[data-choice]");
  if (choice) return onEventChoose(Number(choice.dataset.choice));
});
el("btn-new").addEventListener("click", onNewBattle);
el("btn-again").addEventListener("click", onNewBattle);
el("btn-review").addEventListener("click", () => { endScreen.hidden = true; });
el("btn-continue").addEventListener("click", onContinue);
el("btn-mute").addEventListener("click", () => {
  if (!window.RoyanAudio) return;
  RoyanAudio.setMuted(!RoyanAudio.isMuted());
  syncMute();
});
el("btn-codex").addEventListener("click", openCodex);
el("btn-codex2").addEventListener("click", openCodex);
el("btn-howto").addEventListener("click", openTutorial);
el("btn-tutorial-close").addEventListener("click", closeTutorial);
el("btn-codex-close").addEventListener("click", closeCodex);
el("codex-tabs").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-ctab]");
  if (!btn) return;
  codexTab = btn.dataset.ctab;
  renderCodex();
});
el("codex-class").addEventListener("change", renderCodex);
el("banter").addEventListener("click", () => {
  clearTimeout(banterTimer);
  el("banter").hidden = true;
});
el("modetoggle").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-mode]");
  if (!btn) return;
  mode = btn.dataset.mode;
  document.querySelectorAll("#modetoggle .modebtn").forEach((b) => {
    b.classList.toggle("on", b === btn);
  });
});
document.addEventListener("keydown", (e) => {
  unlockAudio();
  if (e.key === "Escape") {
    if (!el("tutorial-screen").hidden) {
      closeTutorial();
      return;
    }
    if (!el("codex-screen").hidden) {
      closeCodex();
      return;
    }
    if (!warband.hidden) {
      warband.hidden = true;
      return;
    }
    if (selected !== null) {
      selected = null;
      if (snap && !snap.over) {
        renderHand(snap);
        renderFoes(snap);
        updateTargetBanner();
      }
    }
    return;
  }
  if (busy || !snap || snap.over) return;
  if ((e.key === "e" || e.key === "E") && stage.hidden && !table.hidden) {
    onEndTurn();
    return;
  }
  if (e.key >= "1" && e.key <= "9") {
    if (!stage.hidden) {
      const picks = [...stageBody.querySelectorAll("[data-pick]")];
      const want = picks.find((p) => p.dataset.pick === e.key);
      if (want) want.click();
    } else if (!table.hidden && snap.hand) {
      const idx = Number(e.key) - 1;
      if (idx < snap.hand.length) onCard(snap.hand[idx].index);
    }
  }
});
document.addEventListener("pointerdown", unlockAudio);

syncMute();
maybeShowTutorial();
fetch("/api/state").then((r) => r.json()).then(render).catch((e) => {
  selectScreen.hidden = false;
  toast("Could not reach the helm: " + e.message);
});
