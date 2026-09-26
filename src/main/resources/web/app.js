"use strict";
/* Royan browser client (W1): hero select + single-battle combat. No dependencies. */

const el = (id) => document.getElementById(id);
const table = el("table");
const logpanel = el("logpanel");
const selectScreen = el("select-screen");
const endScreen = el("end-screen");

let snap = null;
let prevHp = new Map();
let selected = null;
let busy = false;

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

function fighterHpHtml(f, key) {
  const pct = f.maxHp > 0 ? Math.max(0, Math.round((100 * f.hp) / f.maxHp)) : 0;
  return `<div class="bar"><div class="fill ${hpClass(f.hp, f.maxHp)}" style="width:${pct}%"></div></div>
    <div class="hp-line"><span>${f.hp} / ${f.maxHp}</span>
    ${f.block > 0 ? `<span class="block-chip">BLK ${f.block}</span>` : ""}
    ${statusLine(f)}</div>`;
}

function renderFoes(s) {
  const host = el("foes");
  host.innerHTML = s.enemies.map((f) => {
    const targetable = selected !== null && !f.alive ? "" : (selected !== null ? " targetable" : "");
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

function renderHero(s) {
  const h = s.hero;
  el("hero").innerHTML = `
    <div class="foe-head"><span class="hero-name">${escapeHtml(h.name)}</span>
      <span class="foe-badges"><span class="badge aspect-${h.aspect}">${escapeHtml(h.aspect)}</span></span></div>
    <div class="hero-sub">${escapeHtml(titleCase(s.heroClass))} &middot; Turn ${s.turn}</div>
    <div data-hpkey="hero">${fighterHpHtml(h)}</div>`;
  el("turnbox").innerHTML = `
    <div class="energy">${s.energy} energy</div>
    <div class="piles">Draw ${s.drawCount} &middot; Discard ${s.discardCount}</div>
    <button id="btn-end" class="btn brass" type="button" ${s.over ? "disabled" : ""}>End turn</button>`;
  el("btn-end").addEventListener("click", onEndTurn);
}

function renderHand(s) {
  const host = el("hand");
  host.innerHTML = s.hand.map((c) => `
    <div class="card ${c.playable ? "" : "unplayable"} ${selected === c.index ? "selected" : ""}" data-card="${c.index}">
      <div class="card-top"><span class="cost">${c.cost}</span>
        <span class="card-name">${escapeHtml(c.name)}</span></div>
      <div class="card-kind">${escapeHtml(c.type)} &middot; ${escapeHtml(c.aspect)}</div>
      <div class="card-text">${escapeHtml(c.text)}</div>
      ${c.flavor ? `<div class="card-flavor">${escapeHtml(c.flavor)}</div>` : ""}
    </div>`).join("");
}

function renderLog(s) {
  const log = el("log");
  log.innerHTML = s.log.map((line) => `<div>${escapeHtml(line)}</div>`).join("");
  log.scrollTop = log.scrollHeight;
}

function renderSelect(s) {
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
    host.closest(".foe, .hero").appendChild(span);
    setTimeout(() => span.remove(), 1200);
  };
  check("hero", s.hero.hp);
  s.enemies.forEach((f) => check("foe:" + f.index, f.hp));
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

function render(s) {
  snap = s;
  if (s.error) toast(s.error);
  if (s.phase === "select") {
    table.hidden = true;
    logpanel.hidden = true;
    endScreen.hidden = true;
    selectScreen.hidden = false;
    renderSelect(s);
    updateChrome(s);
    return;
  }
  selectScreen.hidden = true;
  table.hidden = false;
  logpanel.hidden = false;
  if (s.over) selected = null;
  renderFoes(s);
  renderHero(s);
  renderHand(s);
  renderLog(s);
  updateChrome(s);
  showFloaters(s);
  updateTargetBanner();
  if (s.phase === "over") {
    el("end-eyebrow").textContent = s.victory ? "The sky-lane is yours" : "The sea claims another";
    el("end-title").textContent = s.victory ? "Victory" : "Defeat";
    el("end-text").textContent = s.victory
      ? `The Guild toasts ${s.heroName}: ${s.enemies.length} foes broken in ${s.turn} turns. The full three-act campaign sets sail in W2.`
      : `${s.heroName} fell on turn ${s.turn}. The Guild will sing of this voyage — and fund the next one.`;
    endScreen.hidden = false;
  } else {
    endScreen.hidden = true;
  }
}

function updateTargetBanner() {
  const banner = el("target-banner");
  if (selected === null || !snap || snap.phase !== "battle") {
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

async function onSail(heroId) {
  if (busy) return;
  busy = true;
  try {
    const raw = el("seed-input").value.trim();
    const body = { heroClass: heroId };
    if (raw !== "") {
      const seed = Number(raw);
      if (Number.isFinite(seed)) body.seed = Math.trunc(seed);
    }
    selected = null;
    prevHp.clear();
    render(await post("/api/new-battle", body));
  } catch (e) {
    toast("Could not reach the helm: " + e.message);
  } finally {
    busy = false;
  }
}

async function onCard(index) {
  if (busy || !snap || snap.phase !== "battle") return;
  const card = snap.hand.find((c) => c.index === index);
  if (!card || !card.playable) return;
  if (card.needsTarget) {
    selected = selected === index ? null : index;
    renderHand(snap);
    renderFoes(snap);
    updateTargetBanner();
    return;
  }
  busy = true;
  try {
    selected = null;
    render(await post("/api/play", { hand: index, target: -1 }));
  } catch (e) {
    toast("Could not reach the helm: " + e.message);
  } finally {
    busy = false;
  }
}

async function onFoe(index) {
  if (busy || selected === null || !snap || snap.phase !== "battle") return;
  const foe = snap.enemies.find((f) => f.index === index);
  if (!foe || !foe.alive) {
    toast("That foe is already down.");
    return;
  }
  busy = true;
  try {
    const hand = selected;
    selected = null;
    render(await post("/api/play", { hand, target: index }));
  } catch (e) {
    toast("Could not reach the helm: " + e.message);
  } finally {
    busy = false;
  }
}

async function onEndTurn() {
  if (busy || !snap || snap.phase !== "battle") return;
  busy = true;
  try {
    selected = null;
    render(await post("/api/end-turn", {}));
  } catch (e) {
    toast("Could not reach the helm: " + e.message);
  } finally {
    busy = false;
  }
}

function onNewBattle() {
  selected = null;
  endScreen.hidden = true;
  selectScreen.hidden = false;
}

el("hand").addEventListener("click", (e) => {
  const card = e.target.closest("[data-card]");
  if (card) onCard(Number(card.dataset.card));
});
el("foes").addEventListener("click", (e) => {
  const foe = e.target.closest("[data-foe]");
  if (foe && selected !== null) onFoe(Number(foe.dataset.foe));
});
el("btn-new").addEventListener("click", onNewBattle);
el("btn-again").addEventListener("click", onNewBattle);
el("btn-review").addEventListener("click", () => { endScreen.hidden = true; });
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape" && selected !== null) {
    selected = null;
    if (snap && snap.phase === "battle") {
      renderHand(snap);
      renderFoes(snap);
      updateTargetBanner();
    }
  } else if ((e.key === "e" || e.key === "E") && snap && snap.phase === "battle" && !busy) {
    onEndTurn();
  } else if (e.key >= "1" && e.key <= "9" && snap && snap.phase === "battle" && !busy) {
    const idx = Number(e.key) - 1;
    if (idx < snap.hand.length) onCard(snap.hand[idx].index);
  }
});

fetch("/api/state").then((r) => r.json()).then(render).catch((e) => {
  selectScreen.hidden = false;
  toast("Could not reach the helm: " + e.message);
});
