# Particle polish — NexVisuals 1.2.0-dev

Minecraft **1.21.11**, Fabric, Java **21+**. This extends the existing module/config/profile/ClickGUI framework and native particle pipeline. No game/window was started for this pass; visual appearance and measured performance require manual testing.

## Inventory and scope

All eleven existing world-sprite owners were reviewed: Hit Effects, Classic Particle Style, Player Trails, Fireflies, Elytra Trails, Projectile Trails, Jump / Landing Rings, Footstep FX, Totem Echo, Block Interaction FX and Cosmetic Orbitals. Player Trails also owns Ribbon / Dual / Line meshes. Other particle-like effects are first-person Weapon Trails, sky stars/meteors, Live Background motes, Console Menu pixels, container click/hover/transfer feedback, Weather Lens drops and Underwater suspended specks.

Local Trajectory's bounded points/line/block-impact ring was reviewed and preserved: its existing size, fade, opacity, distance/time caps and local-only prediction remain unchanged. Hat geometry, HUD/crosshair controls and post-processing grain are not particle emitters. Existing vanilla particles from totem/item-use/block actions remain Minecraft-owned; this pass decorates them rather than replacing gameplay effects.

## Defaults and old presets

| World effect | Previous fresh size | New fresh/reset size | Increase |
| --- | ---: | ---: | ---: |
| Hit Effects multiplier | 1.0 | 1.18 | 18% |
| Classic Particle Style | .060 | .075 | 25% |
| Player Trails | .130 | .160 | 23% |
| Fireflies | .045 | .056 | 24% |
| Elytra Trails | .100 | .125 | 25% |
| Projectile Trails | .075 | .095 | 27% |
| Jump / Landing Rings radius | 1.30 | 1.40 | 8% |
| Footstep FX | .130 | .160 | 23% |
| Totem Echo | .120 | .145 | 21% |
| Block Interaction FX | .120 | .145 | 21% |
| Cosmetic Orbitals | .090 | .110 | 22% |

These changes affect fresh/reset settings and new recipes. Existing saved sizes are not rewritten. `preservePresetDefaults` freezes omitted old fields before new recipes are appended; original recipe names, explicit values and effective old defaults remain intact. A fixture captured before this pass checks all old fields of fourteen modules, including Weapon Trails, Skybox and Live Background. Extra tests cover neutral old Weather Lens / Underwater settings. Existing night colors, sun/moon paths, star geometry and atmospheric presets are preserved.

The new controls default to neutral/Legacy: no forced rainbow, larger counts, added spread or global opacity increase. Readability comes from moderate size increases and opt-in new combinations of size, shape, fade windows and restrained palettes.

## Common per-module controls

`ParticleAppearance` registers settings with its owning module, so config/profiles/default reset use the existing codecs. FREE effects expose velocity multiplier, drag/retention, gravity offset, extra spread, pattern randomness and velocity variance. Ground/analytic ambient patterns hide inappropriate ballistic controls. Each owner retains its existing amount/density, size, lifetime, speed, spread/radius or height controls.

- Colors: Legacy, Static, Two Color (choose an endpoint per particle), Gradient over life, opt-in Rainbow, NexVisuals Theme accent and Random Between endpoints. Rainbow has speed, saturation, brightness and hue offset. Existing primary/secondary ARGB controls remain; opacity multiplier and RGB brightness are independent.
- Appearance: Preset shape or Soft, Spark, Dot, Star, Ring, Streak, Pixel, Diamond, Heart; controlled size/lifetime variation; light intensity from native lighting to full-bright cosmetic sprites. This is not world illumination or bloom.
- Animation: optional fade-in/fade-out windows, optional start/end scale multipliers, extra spin and distance fade. Legacy shape/fade/scale behavior remains selected until explicitly overridden.
- Advanced: maximum render distance 8–96 blocks, further capped by global particle quality. Fade is applied across the outer 30% when selected. Pattern randomness zero removes randomized motion/spawn/rotation and variance; intentionally authored helixes/orbits remain deterministic.

Specific additions: Fireflies has twinkle strength/speed; Orbitals has point persistence; Jump Rings has 1–4 ring layers (0 keeps the original count) and 0–16 rune marks. Footstep ballistic controls appear only for petal/spark modes. Player ribbon modes use the common color/opacity/brightness/distance controls and hide sprite-only physics. Geometric Weapon Trails uses color-only `ParticlePalette`, opacity/brightness/rainbow/theme and an adjustable tail fade curve; its bounded blade sampling remains intact.

Procedural/GUI effects use appropriate controls rather than the world-particle engine: Skybox has meteor width/opacity/lifetime/speed/tint; Live Background has mote size/opacity/drift/twinkle/independent color; Console Menu has pixel amount/size/opacity/speed/independent color; containers have frame/double-frame/diamond click shape, expansion, opacity, thickness, lifetime and gradient. Weather Lens has droplet size, placement variation and tint; Underwater has speck size/density/drift/shape/color. Container slots and hit areas never move; uncertain transfers retain their existing source-feedback fallback.

## Added recipes — old recipes retained

| Module | New recipes |
| --- | --- |
| Hit Effects | Clean Plus, Energy Bloom, Critical Stars |
| Classic Particle Style | Classic Plus, Pearl Dust, Pixel Confetti |
| Player Trails | Smooth Plus, Neon Comet, Minimal Dots |
| Fireflies | Lantern Meadow, Starlit Garden |
| Elytra Trails | Silkstream, Stardust Flight |
| Projectile Trails | Comet Plus, Crystal Flight |
| Jump / Landing Rings | Luminous Halo, Runic Bloom |
| Footstep FX | Soft Steps, Crystal Steps |
| Totem Echo | Celestial Helix, Crystal Nova |
| Block Interaction FX | Crystal Plus, Workshop Flare |
| Cosmetic Orbitals | Pearl Orbits, Crystal Crown |
| Weapon Trails | Pearl Sweep, Accent Edge |
| Skybox | Meteor Garden |
| Live Background | Stardust |
| Console Menu | Starlit Stone |
| Container Visuals | Crystal Touch, Double Echo |
| Weather Lens | Pearl Glass |
| Underwater FX | Pearl Lagoon, Crystal Depths |

Names such as Critical Stars describe the cosmetic pattern, not confirmation of a critical hit. Hit Effects still reflects the same visible local attack attempt.

## Two distinct new modules

**Consumption FX**: pearls/petals/mist around the local mouth/consumable while the actual EAT/DRINK animation runs. Density 1–3 per four ticks, size .025–.16, lifetime 5–35 ticks, drift/spread, independent eat/drink toggles and optional finish count 0–16, plus common tuning. Recipes: Pearl Sip, Tea Steam, Petal Feast. The finish flourish requires observing the countdown reach its last tick; it is cosmetic feedback, not proof of server consumption. Cancellation/world changes clear observations. No item, use duration or inventory mutations.

**Rain Ripples**: expanding world-space ground/water rings and optional tiny droplets. Recipes: Silver Rain, Quiet Water, Pearl Shower. Editable surface selection, density, radius, size, lifetime, palette and common ground tuning. At most two surface attempts per tick; loaded chunk lookup with `create=false`, actual rain/biome checks, upward-facing collision surface and eye-to-impact block visibility ray. Skips snow, shelter, lava and unseen surfaces; native depth applies after emission. This is a surface interaction effect, whereas existing Weather Lens decorates the screen image. No simulated weather or entity tracking.

## Rendering and performance

One `EffectEmitter` admits world sprites to Minecraft's existing translucent atlas batching. `EffectParticle` and analytic `FireflyParticle` share appearance/extraction logic; the redundant Classic-specific particle class was removed. New Dot/Diamond/Streak masks are original procedural CC0 assets; the original eight masks were not regenerated. Immutable tuning objects are cached by settings revision and shared until an edit, with no disk reads or new textures/framebuffers each particle frame.

| General → Particle quality | Shared spawns/tick | Native live EffectParticle cap | Render distance cap |
| --- | ---: | ---: | ---: |
| Low | 32; evenly thinned | 256 | 32 blocks |
| Medium | 80; evenly thinned | 512 | 48 blocks |
| High (default) | 128 | 768 | 64 blocks |
| Ultra | 192 | 1024 | 96 blocks |

Individual settings are preserved when changing quality. Native Decreased caps admission at 48/tick; Minimal suppresses world-particle spawning. Fireflies retain their separate native 96-live cap, but consume the shared emission budget. Quality-switch remnants keep their original cap until expiry, so temporarily more than the selected live cap may coexist; all cap groups and lifetimes are bounded. Maximum cosmetic life is 200 ticks, base radius 2 and animated radius 3 blocks. Distance culling happens before allocation and again before quad extraction; native frustum/depth/batching remain active. No separate object pool is added: Minecraft owns disposal and render-state reuse.

Projectile Trails keeps 32 tracked visible projectiles, 48 emissions/tick and eight per projectile. Ground/local action loops, ribbon histories (64 player / 48 weapon per hand), up to three geometry subdivisions, container feedback queues (32 ghosts / six hovers), 4000 stars and one procedural meteor remain bounded. Procedural background/rain/silt work has constant grid/layer cost rather than lists of objects. Global particle quality applies to world sprite emission, not sky stars, wallpaper pixels or post-processing grids, whose own caps/settings remain independent.

Settings changes affect new world particles immediately; existing particles finish with their cached spawn style, at most ten seconds. This avoids per-particle live-setting searches and abrupt mid-flight edits. Meshes and GPU/GUI controls update live. GUI reuses section selection, reset, presets and typed controls; unrelated fields are hidden, including rainbow controls in static modes. Visibility changes after sliders are applied once a drag ends, preserving input lifecycle and scroll.

## Manual checks

Compare an old saved profile and old presets before trying new recipes. Check Hit shapes/fade/scale; F5 Player/Elytra/Footstep/Orbitals patterns; bow/pearl Projectile Trails; Jump ring counts 1–4 and Runic amount; real local Totem Echo; break/place Block FX; Fireflies at dusk and indoors; Weapon ribbon alignment/fade. Try each new recipe, Theme/Rainbow/Static and randomness zero, disable/re-enable, distance fade and all quality levels. Test Consumption FX for food/drink, cancellation and completion; Rain Ripples on slabs/ground/water in real rain and confirm none through shelter/walls or in snow.

Check Night Sky and Meteor Garden, each wallpaper including Vanilla fallback, Console Menu reduced motion, container clicks/quick moves, Weather Lens and submerged specks. Verify large/small GUI scales, search/settings scrolling, profile save/load and restart. Measure FPS on the same scene before/after; Sodium/Iris compatibility is not certified without runtime tests. Existing GPU compatibility guards are preserved. This pass does not claim a shader pack, selective bloom, gameplay illumination or visual testing.
