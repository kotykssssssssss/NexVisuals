# Changelog

## 1.2.0-dev — 2026-10-04

- Polished eleven existing world-particle modules through the existing vanilla particle pipeline. Added per-module shape/color modes, fade/scale curves, controlled randomness, motion, glow/light, distance settings and opt-in new recipes. Modestly increased fresh/reset particle sizes while freezing every old preset's effective values and retaining saved user sizes.
- Added shared Low/Medium/High/Ultra emission limits, cached immutable style snapshots, distance culling and original Dot/Diamond/Streak atlas masks. Consolidated Classic Particle Style into the same emitter; retained its native generic sprite, friction and fade defaults.
- Extended geometric Weapon Trails, sky meteors, GPU background motes, Console Menu pixels, container click ripples, Weather Lens droplets and Underwater specks with appropriate controls. Added new recipes without replacing old styles or the successful night sky.
- Added Consumption FX for observed local eat/drink animations and Rain Ripples on loaded, exposed, visible ground/water during actual rain. No new Mixins, dependencies, item/packet/weather changes or remote prediction.
- Added conditional setting visibility to the existing metadata-driven editor, including safe updates after sliders finish dragging. Added old-preset snapshots, particle math/budget, persistence, use-animation and rendering-contract checks. Minecraft remains strictly 1.21.11.

## 1.1.1-dev — 2026-10-04

- Fixed a crash when opening module or General settings between client ticks: pending widget rebuilds now run before drawing the next frame or routing the next input event.
- Kept rebuilds deferred until the current widget callback returns, preserving safe iteration, search focus and stable scrolling. Reset stale editor controls and dragging on rebuild.
- Added headless regression coverage for mouse/keyboard transitions, enabled/disabled modules, coalesced requests and actual screen lifecycle ordering. Minecraft remains strictly 1.21.11.

## 1.1.0-dev — 2026-10-04

- Reworked the configuration screen into movable, collapsible category windows with independent scrolling, global search and compact toggle/settings rows. All existing modules use the same metadata-driven settings editor, presets, colors, keybinds, profiles and HUD editor.
- Persisted normalized category placement and folded state in optional schema-1 GUI fields; retained automatic placement for old configs and high-GUI-scale pagination.
- Added Local Trajectory: held-item, local-only air prediction for charged bows, single-arrow loaded crossbows, pearls, snowballs, eggs, splash/lingering potions and non-Riptide tridents. Uses native block clipping, bounded prediction and depth-tested ribbon/points/impact rings; stops at unsupported conditions and never scans entities.
- Added focused physics, native integration order, GUI layout/input/scroll, configuration migration and profile persistence tests. No new Mixins or dependencies; Minecraft remains strictly 1.21.11.

## 1.0.0 — 2026-10-04

- Added Projectile Trails (Comet / Embers / Pearl Halos), bounded tracking and emission through existing vanilla particle batching.
- Added draggable Pickup HUD (Stacked / Compact / Minimal), confirmed local pickup observation, count merging and elapsed-time UI transitions.
- Added sound build-mode status to the existing GUI. Preserved the six supplied clips in the local build; prepared a separate public build with vanilla event fallbacks.
- Added MIT Fabric metadata and packaged existing license/asset notices. Prepared release notes and separate public/local artifacts.
- Retained previous search, Viewmodel, Swing and Trails fixes; configuration schema and Minecraft 1.21.11 target remain unchanged.

## 0.9.1-dev — 2026-10-02

- Prevented repeated module-list rebuilds and scroll resets on search cursor notifications, fixing flickering descriptions while filtering.

## 0.9.0-dev — 2026-10-01

- Corrected PvP/Cinematic Viewmodel layouts and the Cinematic global profile; added optional left-handed mirroring.

## 0.8.2-dev — 2026-10-01

- Reworked cosmetic Swing playback and aligned Weapon Trails to actual item transforms with bounded smoothing/history.

## 0.8.1-dev — 2026-10-01

- Fixed first-frame Player Trails crash by using the prepared Fabric world draw phase and guarding missing context data.

Earlier features and validation history are documented in [features](docs/features.md) and [validation](docs/validation.md).
