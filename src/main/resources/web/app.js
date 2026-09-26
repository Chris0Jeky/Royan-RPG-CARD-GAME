"use strict";
/* Royan browser client (W2): campaign runs + quick skirmishes. No dependencies. */

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
let selected = null;
let busy = false;
let mode = "run";
let knownSave = false;

function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
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
  return `<div class="card ${cls || ""}" ${attr || ""}>
      <div class="card-top"><span class="cost">${c.cost}</span>
        <span class="card-name">${escapeHtml(c.name)}</span></div>
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
      <div class="foe-head"><span class="foe-name">${escapeHtml(f.name)}</span>
        <span class="foe-badges">
          <span class="badge aspect-${f.aspect}">${escapeHtml(f.aspect)}</span>
          <span class="badge row-badge">${f.row === "BACK" ? "BACK" : "FRONT"}</span>
        </span></div>
      ${fighterHpHtml(f)}
      ${f.alive ? `<span class="intent intent-${f.intent.kind}">${escapeHtml(intentLabel(f.intent))}</span>` : `<span class="intent">DOWN</span>`}
      ${f.flavor ? `<div class="foe-flavor">${escapeHtml(f.flavor)}</div>` : ""}
    </div>`;
  }).join("");
}

function renderAllies(s) {
  const host = el("allies");
  if (!s.companions || !s.companions.length) {
    host.innerHTML = "";
    return;
  }
  host.innerHTML = s.companions.map((a) => `
    <div class="ally" data-hpkey="ally:${a.index}">
      <span class="aname">${escapeHtml(a.name)}</span>
      ${fighterHpHtml(a)}
    </div>`).join("");
}

function renderHero(s) {
  const h = s.hero;
  const cls = s.heroClass ? titleCase(s.heroClass) : (s.run ? titleCase(s.run.heroClass) : "");
  el("hero").innerHTML = `
    <div class="foe-head"><span class="hero-name">${escapeHtml(h.name)}</span>
      <span class="foe-badges"><span class="badge aspect-${h.aspect}">${escapeHtml(h.aspect)}</span></span></div>
    <div class="hero-sub">${escapeHtml(cls)} &middot; Turn ${s.turn}</div>
    <div data-hpkey="hero">${fighterHpHtml(h)}</div>`;
  el("turnbox").innerHTML = `
    <div class="energy">${s.energy} energy</div>
    <div class="piles">Draw ${s.drawCount} &middot; Discard ${s.discardCount}</div>
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
    <span class="coin">${r.gold}g</span><span>${r.dust} dust</span><span>${r.shards} shards</span>
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

function renderWarband() {
  const r = snap.run;
  warband.innerHTML = `
    <button class="btn ghost closebtn" id="btn-closewb" type="button">Close</button>
    <h2>War-band</h2>
    <h3>Deck (${r.deckSize})</h3>
    <ul>${r.deck.map((c) => `<li>${c.cost} — ${escapeHtml(c.name)} <em>${escapeHtml(c.type)}</em></li>`).join("")}</ul>
    <h3>Relics (${r.relics.length})</h3>
    <ul>${r.relics.length ? r.relics.map((x) => `<li><strong>${escapeHtml(x.name)}</strong> (${escapeHtml(x.effect)} ${x.value})<br><em>${escapeHtml(x.flavor || "")}</em></li>`).join("") : "<li><em>No relics yet.</em></li>"}</ul>
    <h3>Companions (${r.companions.length})</h3>
    <ul>${r.companions.length ? r.companions.map((c) => `<li><strong>${escapeHtml(c.name)}</strong> — ${escapeHtml(c.role)} (${c.hp}/${c.maxHp})</li>`).join("") : "<li><em>None yet. Taverns hire blades.</em></li>"}</ul>`;
  warband.hidden = false;
  el("btn-closewb").addEventListener("click", () => { warband.hidden = true; });
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
      if (!isOpt) {
        return `<div class="${cls}"><span class="ntype ntype-${n.type}">${n.type}</span><span class="nid">${escapeHtml(n.id)}${isHere ? " — here" : ""}</span></div>`;
      }
      pickNo++;
      return `<button class="${cls}" type="button" data-node="${escapeHtml(n.id)}" data-pick="${pickNo}"><span class="ntype ntype-${n.type}"><span class="picknum">${pickNo}</span>${n.type}</span><span class="nid">${escapeHtml(n.id)}</span></button>`;
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
      <div class="sidecard"><h3>${escapeHtml(sh.relic.relic.name)}</h3>
        <p>${escapeHtml(sh.relic.relic.effect)} ${sh.relic.relic.value} &middot; ${escapeHtml(sh.relic.relic.rarity)}<br><em>${escapeHtml(sh.relic.relic.flavor || "")}</em></p>
        <button class="buybtn" type="button" data-buyrelic="1">Buy relic — ${sh.relic.price}g</button>
      </div>`
    : `<div class="sidecard"><h3>Relic vault</h3><p>Nothing left but dust.</p></div>`;
  return `<h2>Sky-dock shop</h2>
    <p class="lede">Purse: <strong>${s.run.gold}g</strong>. The keep polishes glass and waits.</p>
    <div class="shopgrid">${stock}</div>
    <div class="shopside">${relic}
      <div class="sidecard"><h3>Patch up</h3><p>Healers close your wounds: +${sh.healAmount} HP.</p>
        <button class="buybtn" type="button" data-buyheal="1">Heal — ${sh.healCost}g</button></div>
      <div class="sidecard"><h3>Weigh anchor</h3><p>Back to the open sky.</p>
        <button class="btn ghost" type="button" data-leave="shop">Leave shop</button></div>
    </div>`;
}

function stageTavern(s) {
  const t = s.tavern;
  const r = s.run;
  const card = (action, title, desc) => `
    <div class="sidecard"><h3>${title}</h3><p>${desc}</p>
      <button class="buybtn" type="button" data-tavern="${action}">Choose</button></div>`;
  return `<h2>The Gilded Gale</h2>
    <p class="lede">Purse: <strong>${r.gold}g</strong>, ${r.dust} dust, ${r.shards} shards. War-band: ${r.companions.length}/${r.maxCompanions}.</p>
    <div class="taverngrid">
      ${card("meal", "Hearty meal", `+50% HP for <span class="price">${t.mealCost}g</span>.`)}
      ${card("recruit", "Hire a blade", `A companion for <span class="price">${t.recruitCost}g</span>.`)}
      ${card("remove", "Strike a basic", `Thin the deck for <span class="price">${t.removeCost} dust</span>.`)}
      ${card("relic", "Back-room trade", `A relic for <span class="price">${t.relicGold}g + ${t.relicShards} shard</span>.`)}
    </div>
    <div class="draft-actions"><button class="btn ghost" type="button" data-leave="tavern">Leave tavern</button></div>`;
}

function stageEvent(s) {
  const e = s.event;
  let n = 0;
  return `<h2>${escapeHtml(e.title)}</h2>
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

/* ---------- select + chrome ---------- */

function renderSelect(s) {
  knownSave = !!s.hasSave;
  const host = el("heroes");
  host.innerHTML = s.heroes.map((h) => `
    <button class="hero-pick" type="button" data-hero="${h.id}">
      <h2>${escapeHtml(h.name)}</h2>
      <span class="hp">${h.hp} HP &middot; ${escapeHtml(h.aspect)}</span>
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

function showFloaters(s) {
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduce) return;
  const check = (key, hp) => {
    const prev = prevHp.get(key);
    prevHp.set(key, hp);
    if (prev === undefined || prev === hp) return;
    const host = document.querySelector(`[data-hpkey="${key}"]`);
    if (!host) return;
    const diff = hp - prev;
    const span = document.createElement("span");
    span.className = "floater" + (diff > 0 ? " heal" : "");
    span.textContent = (diff > 0 ? "+" : "") + diff;
    host.closest(".foe, .hero, .ally").appendChild(span);
    setTimeout(() => span.remove(), 1200);
  };
  check("hero", s.hero.hp);
  s.enemies.forEach((f) => check("foe:" + f.index, f.hp));
  (s.companions || []).forEach((a) => check("ally:" + a.index, a.hp));
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
  if (s.mode === "run" && s.abandoned) {
    el("end-eyebrow").textContent = "Charts held by the Guild";
    el("end-title").textContent = "Campaign Suspended";
    el("end-text").textContent = `Act ${r.act}, level ${r.level}. Resume any time with Continue.`;
  } else if (s.mode === "run" && s.victory) {
    el("end-eyebrow").textContent = "The sky-lanes are yours";
    el("end-title").textContent = "Campaign Victory";
    el("end-text").textContent = `${r.heroName}, level ${r.level}: ${r.deckSize} cards, ${r.relics.length} relics, ${r.companions.length} sworn blades. Sung for a hundred years.`;
  } else if (s.mode === "run") {
    el("end-eyebrow").textContent = "The sea claims another";
    el("end-title").textContent = "Defeat";
    el("end-text").textContent = `${r.heroName} fell in Act ${r.act} at level ${r.level}. The Guild funds the next voyage.`;
  } else if (s.victory) {
    el("end-eyebrow").textContent = "The sky-lane is yours";
    el("end-title").textContent = "Victory";
    el("end-text").textContent = `The Guild toasts ${s.heroName}: ${s.enemies.length} foes broken in ${s.turn} turns. Try the full campaign from the menu.`;
  } else {
    el("end-eyebrow").textContent = "The sea claims another";
    el("end-title").textContent = "Defeat";
    el("end-text").textContent = `${s.heroName} fell on turn ${s.turn}. The Guild will sing of this voyage — and fund the next one.`;
  }
  endScreen.hidden = false;
}

function render(s) {
  snap = s;
  if (s.error) toast(s.error);
  warband.hidden = true;
  if (s.phase === "select") {
    table.hidden = true;
    stage.hidden = true;
    logpanel.hidden = true;
    runbar.hidden = true;
    arrivalBox.hidden = true;
    endScreen.hidden = true;
    selectScreen.hidden = false;
    renderSelect(s);
    updateChrome(s);
    return;
  }
  selectScreen.hidden = true;
  logpanel.hidden = false;
  renderLog(s);
  updateChrome(s);
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
    const path = mode === "run" ? "/api/new-run" : "/api/new-battle";
    render(await post(path, seedBody(heroId)));
  });
}

async function onContinue() {
  return guarded(async () => {
    selected = null;
    prevHp.clear();
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
el("modetoggle").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-mode]");
  if (!btn) return;
  mode = btn.dataset.mode;
  document.querySelectorAll("#modetoggle .modebtn").forEach((b) => {
    b.classList.toggle("on", b === btn);
  });
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
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

fetch("/api/state").then((r) => r.json()).then(render).catch((e) => {
  selectScreen.hidden = false;
  toast("Could not reach the helm: " + e.message);
});
