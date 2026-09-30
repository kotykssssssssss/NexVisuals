# Дополнительные visual systems 0.5.0-dev

Цель остаётся **Minecraft Java Edition 1.21.11 / Fabric / Java 21+**. Foundation, night Skybox, Live Background, Shield/Fire, Viewmodel и другие прежние функции сохранены. Здесь описаны восемь новых модулей, а не восемь вариантов одного color filter.

## Какие идеи исследованы

Изучены авторские страницы [Visuality](https://modrinth.com/mod/visuality), [Jump Circle](https://modrinth.com/mod/jump-circle), [Custom Totem Particles](https://modrinth.com/mod/custom-totem-particles), [Totem Pop Effects](https://modrinth.com/mod/totem-pop-effects), [Particle Rain](https://modrinth.com/mod/particle-rain) и предоставленная пользователем [Box Client](https://modrinth.com/mod/boxclient). Они служили источниками направлений: формы/движение cosmetic particles, ground rings, локальная totem decoration, реакции на environment, независимые visual modules. Их код, textures, audio и packs не копировались и не стали зависимостями NexVisuals.

Сайт [PulseVisuals](https://pulsevisuals.pro/) вернул только JavaScript shell; предоставленный GitHub README не удалось получить через web tool. Поэтому утверждений о проверенном каталоге Pulse здесь нет. Список пользователя использован как набор идей; функции, требующие скрытой информации или изменения gameplay, не добавлялись.

Проверен [публичный Iris API 1.21.11](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java). Интеграция сохраняет прежний `isShaderPackInUse` guard. Управление private shader-pack settings, установка/скачивание pack и собственный полный shadows/water/shader engine не добавлены. Три новых эффекта действительно используют GPU shader, но **не являются полноценным shader pack**.

## Модули и настройки

| Категория / модуль | Пресеты и различия | Основные настройки |
| --- | --- | --- |
| Particles → Jump / Landing Rings | **Double Halo**: две expanding ground waves; **Runic**: ring + 8 star runes; **Quiet**: один небольшой короткий ring | Shape, on jump/landing, два ARGB colors, radius, lifetime. Landing size реагирует на наблюдаемое падение. |
| Particles → Footstep Effects | **Light Prints**: чередующиеся footprint marks; **Petal Walk**: две поднимающиеся petal arcs; **Ember Steps**: три короткие искры; **Ripples**: expanding circles | Style, два ARGB colors, distance/stride, size, lifetime, optional third-person only. Стоя на месте, в воздухе и воде ничего не испускает. |
| Particles → Totem Echo | **Golden Helix**: две восходящие спирали; **Phoenix**: upward spark fountain с gravity; **Supernova**: radial starburst | Style, colors, amount, size, spread, speed, lifetime. Только реальное срабатывание тотема у local player; vanilla animation/sound остаются. |
| Particles → Block Interaction FX | **Crystal**: cool star shards; **Workshop**: короткие golden sparks; **Garden**: медленные petal arcs | Break/placement independently, style, colors, amount, size, spread speed, lifetime. Частицы placement появляются снаружи нового блока. |
| World → Cosmetic Orbitals | **Atom**: tilted intersecting paths; **Starlight Crown**: круг звёзд над головой; **Spiral**: вертикальная helix | Path, colors, count, radius, height, size, turns per second. Только видимый local player в third person. |
| Post Processing → Weather Lens | **Drizzle**: небольшие lens drops; **Storm Glass**: вытянутые быстрые rivulets; **Mist**: мелкие капли с очень слабой refraction | Pattern, intensity, density, fall speed, refraction pixels, highlights, wet/dry transition. Только actual rain у exposed camera; под крышей и в сухих/снежных биомах эффект затухает. |
| Post Processing → Underwater FX | **Quiet Water**: мягкие ripples/shimmer; **Lagoon**: более выраженная refraction/caustic pattern; **Deep**: медленные ripples и больше specks без shimmer | Intensity, refraction pixels, motion speed, caustic amount/scale, silt, transition. Только реальная камера в WATER; vanilla fog остаётся. |
| Post Processing → Retro Display | **Console CRT**: небольшой pixel grid + scanlines + phosphor; **Pixel Adventure**: крупные пиксели, restricted colors, Bayer dither; **Clean CRT**: native resolution с CRT surface; **Soft Mosaic**: крупные пиксели без quantization/scanlines | Intensity, pixel size, color levels, ordered dither, scanline strength/spacing, RGB phosphor mask. HUD/GUI остаются резкими. |

Все presets являются наборами обычных typed settings: `Apply` задаёт starting point, ручная правка показывает `Custom`, `Reset settings` возвращает defaults. Старый editor автоматически получает новые модули без отдельных screens. Все настройки/enable сохраняются в `config/nexvisuals.json` и пользовательских profiles. Схема JSON остаётся 1, отсутствующие новые поля/модули безопасно defaults/OFF.

## Rendering и лимиты

- Пять cosmetic modules используют native particle engine, atlas, translucent batching, lighting/fog/depth. Surface quads имеют world-fixed orientation, а прежние billboard particles — прежнюю camera-facing orientation. Footprint mask оригинальная, сгенерирована из формул и CC0; другие masks повторно используются.
- Общий budget **128 particles/tick**, **48** в Decreased, ноль в Minimal. Общий `EffectParticle` cap **768**. Jump event: максимум 9 quads; footstep: максимум два шага за тик и три particles/step; totem: до 96 particles/event; block action: до 32 particles; orbitals: до 12 points раз в два тика, lifetime 4 ticks. Все ограничены ещё и общим budget.
- Нет коллекций истории footsteps/orbits; только последнее movement sample и accumulators. При world change/teleport/disable trackers очищаются. Пять новых модулей не выполняют entity scans.
- `visual_grade` расширен до 17 vec4 / **272-byte UBO**. Lens drops — две аналитические grid layers с малой UV-refraction и multiplicative glass shading. Water — ограниченные sinusoidal UV offsets, аналитический shimmer/silt. Retro — image grid sampling, per-channel quantization, 4×4 Bayer dither, scanline/phosphor masks. Это screen-space techniques, не physical water caustics/reflections.
- Все image modules работают в **одном** существующем fullscreen pass, с одной reused scene copy. Только прежний Wide Glow при необходимости добавляет свой quarter-resolution target/pass. Resize пересоздаёт нужные targets; no readback, disk reads, per-frame texture creation или CPU image processing.
- World image effects перед HUD/UI; меню не пикселизируется и дождь не искажает кнопки. Shader math может сохранять чёрный цвет, но не восстанавливает HDR data. Water shimmer не убирает fog или ограничения видимости.
- Active Iris pack и Blindness/Darkness приостанавливают все NexVisuals world-image passes. Failure даёт один ERROR в log и status в GUI; OFF/ON одного из image modules позволяет retry. Sodium/Iris runtime compatibility и FPS **не проверены**.

## Ограничения

- Игра и визуальная часть этого обновления не запускались. Успешные build, GLSL compile/link и pinned bytecode checks не заменяют Mixin transformation/GPU/runtime testing.
- Шаги — cosmetic marks, не accurate animation tracking каждого foot bone. Они не проецируются на сложный terrain mesh. Ground waves рисуются на фактической высоте feet, возможны clipping на slopes/slabs/edges; имеют обычную глубину и не должны просвечивать стены.
- Orbitals — небольшой constellation из particles, не непрерывный mesh ribbon или физический свет. Частицы могут слегка запаздывать за быстро движущимся игроком в течение своего 4-tick lifetime.
- Block FX отражает client prediction, а не server-confirmed placement. Если сервер отклонит действие, короткий cosmetic burst уже мог появиться. Redirected contexts (например scaffolding) и альтернативные block types (например wall sign) могут пропускаться. Вёдра и удалённые игроки не отслеживаются. Opening/toggling a block само по себе не является placement.
- Дождь на линзе и underwater caustics не моделируют реальные объекты/освещение; это bounded screen effects. Нет rain puddles, depth-aware wetness, water mesh replacement, HDR, shadows, SSR, volumetrics или настоящего shader-pack pipeline.
- Новый cap/quality уважает Particle settings Minecraft: Minimal выключает новые particles. Resource packs могут менять atlas/vanilla appearance. Дополнительной сетевой синхронизации cosmetics нет.

## Ручной checklist

1. **Jump / Landing Rings:** F5, прыжок, падение с небольшой высоты, ходьба со ступени, teleport, slopes/slabs, все три presets, colors/radius/lifetime, OFF. Первый вход в мир и teleport не должны создавать jump wave.
2. **Footsteps:** разные presets, ходьба/бег, alternating left/right, остановка, вода, прыжок, third-person switch, opaque floor/стены, lifetime и stride. Prints должны выглядеть как две части подошвы, не missing texture.
3. **Totem:** реально активировать свой тотем, сравнить три формы, first/third person. Vanilla hand popup/sound/particles остаются; срабатывание тотема другого игрока не добавляет NexVisuals Echo.
4. **Block FX:** break/place stone, replacement grass/snow layers, creative/survival, обе руки; opening chest/toggling lever с BlockItem не должно считаться placement. Сравнить styles и OFF; server block protection не меняется.
5. **Orbitals:** F5, все три presets, поворот/движение/crouch, радиус/скорость/цвет; first person/invisibility/OFF прекращают emission. Стена должна нормально закрывать particles.
6. **Weather Lens:** дождь снаружи, крыша, desert/snow biome, окончание дождя, вход в воду; Drizzle/Storm Glass/Mist, refraction/intensity=0, smooth transition. HUD и editor читаемы.
7. **Underwater:** вход/выход из воды, Quiet Water/Lagoon/Deep, camera partly submerged, motion/shimmer/silt sliders, pause. Fog и water vision остаются vanilla.
8. **Retro:** четыре presets, native/pixel resolution, ordered dither, scanlines/phosphor, small intensity, window resize. Lightweight Shaders отдельно OFF и ON; HUD и меню не меняют резкость.
9. **Persistence:** сохранить profile, вручную поправить цвет/число (`Custom`), close/restart, load/rename/delete profile, reset. Старые night Skybox, mini Shield/Fire и Live Background должны остаться рабочими.
10. **Limits / compatibility:** все новые effects одновременно, быстрая серия действий, Minimal/Decreased particles, выход/смена мира, выключение всех модулей, resize/resource reload, latest.log, FPS. Повторить с фактическим Sodium и Iris; active shader pack должен показывать paused status для image modules и Skybox.

Результаты automated checks и точный production JAR указаны в [validation](validation.md).
