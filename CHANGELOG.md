# Changelog

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
