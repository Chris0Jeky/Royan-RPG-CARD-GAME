"use strict";
/* Royan audio: synthesized SFX + generative ambient music. No assets, no requests.
   Attaches window.RoyanAudio in browsers; exports stubs under node for smoke tests. */
(function (root, factory) {
  const api = factory();
  if (typeof window !== "undefined") {
    window.RoyanAudio = api;
  }
  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
})(typeof self !== "undefined" ? self : this, function () {
  const MUTE_KEY = "royan-muted";
  const VOLUME_KEY = "royan-volume";
  const MUSIC_KEY = "royan-music";
  const MUSIC_BUS_GAIN = 0.14;
  const store = typeof localStorage !== "undefined" ? localStorage : null;
  let ctx = null;
  let master = null;
  let musicBus = null;
  let muted = false;
  let musicOn = false;
  let dronesStarted = false;
  let volume = 0.9;
  let musicEnabled = true;
  try {
    muted = store !== null && store.getItem(MUTE_KEY) === "1";
    if (store !== null && store.getItem(VOLUME_KEY) !== null) {
      volume = Math.min(1, Math.max(0, Number(store.getItem(VOLUME_KEY))));
      if (!Number.isFinite(volume)) volume = 0.9;
    }
    if (store !== null && store.getItem(MUSIC_KEY) !== null) {
      musicEnabled = store.getItem(MUSIC_KEY) !== "0";
    }
  } catch (e) {
    muted = false;
  }

  function ensure() {
    if (typeof window === "undefined") return false;
    if (ctx) {
      if (ctx.state === "suspended") ctx.resume();
      return true;
    }
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return false;
    ctx = new AC();
    master = ctx.createGain();
    master.gain.value = muted ? 0 : volume;
    master.connect(ctx.destination);
    musicBus = ctx.createGain();
    musicBus.gain.value = musicEnabled ? MUSIC_BUS_GAIN : 0;
    const delay = ctx.createDelay(1);
    delay.delayTime.value = 0.42;
    const feedback = ctx.createGain();
    feedback.gain.value = 0.32;
    const wet = ctx.createGain();
    wet.gain.value = 0.35;
    musicBus.connect(master);
    musicBus.connect(delay);
    delay.connect(feedback);
    feedback.connect(delay);
    delay.connect(wet);
    wet.connect(master);
    return true;
  }

  function tone(freq, dur, type, gain, slideTo, when) {
    if (!ensure()) return;
    const t = when !== undefined ? when : ctx.currentTime;
    const osc = ctx.createOscillator();
    const amp = ctx.createGain();
    osc.type = type || "sine";
    osc.frequency.setValueAtTime(freq, t);
    if (slideTo) osc.frequency.exponentialRampToValueAtTime(slideTo, t + dur);
    amp.gain.setValueAtTime(0.0001, t);
    amp.gain.exponentialRampToValueAtTime(gain || 0.15, t + 0.012);
    amp.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    osc.connect(amp);
    amp.connect(master);
    osc.start(t);
    osc.stop(t + dur + 0.05);
  }

  function noise(dur, filterFreq, gain, type, when) {
    if (!ensure()) return;
    const t = when !== undefined ? when : ctx.currentTime;
    const len = Math.max(1, Math.floor(ctx.sampleRate * dur));
    const buffer = ctx.createBuffer(1, len, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < len; i++) data[i] = Math.random() * 2 - 1;
    const src = ctx.createBufferSource();
    src.buffer = buffer;
    const filter = ctx.createBiquadFilter();
    filter.type = type || "lowpass";
    filter.frequency.value = filterFreq || 1000;
    const amp = ctx.createGain();
    amp.gain.setValueAtTime(gain || 0.2, t);
    amp.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    src.connect(filter);
    filter.connect(amp);
    amp.connect(master);
    src.start(t);
    src.stop(t + dur + 0.02);
  }

  function musicTone(freq, dur, gain) {
    if (!ctx || !musicOn) return;
    const t = ctx.currentTime;
    const osc = ctx.createOscillator();
    const amp = ctx.createGain();
    osc.type = "triangle";
    osc.frequency.value = freq;
    amp.gain.setValueAtTime(0.0001, t);
    amp.gain.exponentialRampToValueAtTime(gain, t + 0.05);
    amp.gain.exponentialRampToValueAtTime(0.0001, t + dur);
    osc.connect(amp);
    amp.connect(musicBus);
    osc.start(t);
    osc.stop(t + dur + 0.05);
  }

  const SCALE = [110, 130.81, 146.83, 164.81, 196, 220, 261.63, 293.66];

  function pluck() {
    if (!musicOn || !ctx) return;
    if (Math.random() < 0.75) {
      musicTone(SCALE[Math.floor(Math.random() * SCALE.length)], 1.6, 0.5);
      if (Math.random() < 0.25) {
        setTimeout(() => musicTone(
          SCALE[Math.floor(Math.random() * SCALE.length)], 1.8, 0.35), 350);
      }
    }
    musicTimer = setTimeout(pluck, 2100 + Math.random() * 2200);
  }

  let musicTimer = null;

  function startMusic() {
    if (musicOn || !ensure()) return;
    musicOn = true;
    if (musicBus) musicBus.gain.value = MUSIC_BUS_GAIN;
    if (dronesStarted) {
      pluck();
      return;
    }
    dronesStarted = true;
    const t = ctx.currentTime;
    [55, 82.41].forEach((freq) => {
      const osc = ctx.createOscillator();
      const amp = ctx.createGain();
      osc.type = "sine";
      osc.frequency.value = freq;
      amp.gain.value = 0.05;
      const lfo = ctx.createOscillator();
      lfo.frequency.value = 0.07;
      const lfoAmp = ctx.createGain();
      lfoAmp.gain.value = 0.02;
      lfo.connect(lfoAmp);
      lfoAmp.connect(amp.gain);
      osc.connect(amp);
      amp.connect(musicBus);
      osc.start(t);
      lfo.start(t);
    });
    pluck();
  }

  function play(name) {
    if (muted) return;
    switch (name) {
      case "card":
        noise(0.09, 3200, 0.12, "bandpass");
        tone(640, 0.07, "triangle", 0.08, 420);
        break;
      case "hit":
        noise(0.14, 750, 0.28);
        tone(130, 0.14, "sine", 0.22, 70);
        break;
      case "block":
        tone(240, 0.08, "square", 0.07, 180);
        noise(0.05, 5200, 0.08, "highpass");
        break;
      case "heal":
        tone(520, 0.28, "sine", 0.12, 784);
        break;
      case "coin":
        tone(988, 0.09, "square", 0.06);
        setTimeout(() => tone(1319, 0.14, "square", 0.05), 80);
        break;
      case "turn":
        tone(330, 0.07, "sine", 0.06);
        break;
      case "draft":
      case "boon":
        tone(660, 0.18, "triangle", 0.1, 990);
        break;
      case "victory":
        [440, 554.37, 659.25, 880].forEach((f, i) => {
          setTimeout(() => tone(f, 0.4, "triangle", 0.14), i * 140);
        });
        break;
      case "defeat":
        tone(110, 0.9, "sine", 0.2, 55);
        noise(0.5, 300, 0.1);
        break;
      case "error":
        tone(160, 0.12, "square", 0.05, 120);
        break;
      default:
        break;
    }
  }

  function unlock() {
    if (ensure() && musicEnabled) startMusic();
  }

  function applyVolume() {
    if (ctx && master) master.gain.value = muted ? 0 : volume;
  }

  function setMuted(value) {
    muted = !!value;
    try {
      if (store) store.setItem(MUTE_KEY, muted ? "1" : "0");
    } catch (e) {
      /* private mode: sound still toggles for the session */
    }
    applyVolume();
  }

  function isMuted() {
    return muted;
  }

  function setVolume(value) {
    volume = Math.min(1, Math.max(0, Number(value)));
    if (!Number.isFinite(volume)) volume = 0.9;
    try {
      if (store) store.setItem(VOLUME_KEY, String(volume));
    } catch (e) {
      /* private mode: volume still applies for the session */
    }
    applyVolume();
  }

  function getVolume() {
    return volume;
  }

  function stopMusic() {
    musicOn = false;
    if (musicTimer) {
      clearTimeout(musicTimer);
      musicTimer = null;
    }
    if (musicBus) musicBus.gain.value = 0;
  }

  function setMusicEnabled(value) {
    musicEnabled = !!value;
    try {
      if (store) store.setItem(MUSIC_KEY, musicEnabled ? "1" : "0");
    } catch (e) {
      /* private mode: music still toggles for the session */
    }
    if (musicEnabled) {
      if (ctx && !musicOn) startMusic();
    } else {
      stopMusic();
    }
  }

  function isMusicEnabled() {
    return musicEnabled;
  }

  return { play, unlock, setMuted, isMuted, setVolume, getVolume, setMusicEnabled, isMusicEnabled };
});
