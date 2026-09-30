# Проверка 0.6.0-dev — 30 сентября 2026

## Автоматически проверено

Цель из Gradle и production metadata: **Minecraft 1.21.11**, не 1.21.1. Java 21 (Oracle 21.0.9), Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11, Loom 1.14.10, official Mojang mappings 1.21.11, Gradle Wrapper 9.2.1. Dependencies не обновлялись; cache/TEMP внутри проекта; build выполнен offline.

Команды из `E:\Projects\nexvisuals`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
$env:TEMP = Join-Path (Get-Location) '.tools\tmp'
$env:TMP = $env:TEMP
.\gradlew.bat test clientTest --offline --console=plain --warning-mode=all '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\gradlew.bat clean build --offline --console=plain --warning-mode=all '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\tools\validate_shaders.ps1 -Validator '.tools\glslang-16.6.0\bin\glslang.exe'
git diff --check
```

- Финальная **clean build успешна за 40 секунд**, 15 tasks выполнены, remapJar/check/verifyModJar прошли. Java/Gradle warnings в финальном output отсутствуют.
- **125 tests: 91 core + 34 client; 0 failures/errors/skipped.** Добавлены 13 focused tests: HSV/RGB round-trip с alpha, clamping/hue wrap/gray-black hue retention, channel/hex edits, health/durability/duration/heartbeat envelopes, reticle motion, четыре реальных HUD definitions/editor settings, presets/config/profile persistence и legacy defaults, malformed settings, atlas contributions и PNG masks. Существующие core/catalog/Mixin contract tests сохранены и прошли.
- **5 GLSL 330 pairs compile/link** и uniform reflection с настоящими Minecraft 1.21.11 includes: SkyConfig 176, StarConfig 48, VisualConfig 272, BackgroundConfig 96, HighlightConfig 16 bytes. Это source/layout validation, не GPU draw. Shader chain на этом этапе не переписан.
- `git diff --check` успешен; новые untracked text files отдельно проверены на trailing whitespace. Production JAR проверен на exact MC/client-only metadata, объявленные Mixin classes, восемь masks, десять GLSL sources, три bundled profiles; без Minecraft/test classes. Новые пять client classes (четыре HUD + ColorPickerScreen) присутствуют в JAR.
- Изменения только внутри NexVisuals; без Minecraft/runClient/computer-use, commit/push/tag/release.

Промежуточные неудачи не скрываются: первая compilation потребовала явно указать String для overloaded Font.width comparator. Следующая headless suite дала 28 failures из одной причины: обращение к ItemStack.EMPTY в constructor каталога запускало Minecraft registries до bootstrap. Обращение перенесено на in-world tick; headless bootstrap не добавлялся. Ещё один новый тест ошибочно выбирал первый classpath particle atlas (Minecraft, где не у каждого source есть sprite); проверка теперь учитывает atlas contributions и их типы. Все причины исправлены, повторный полный suite и финальные clean builds прошли. Последний rebuild дополнительно проверил адаптивную палитру и сохранение прежнего spark spin behavior.

## Production JAR

**`E:\Projects\nexvisuals\build\libs\nexvisuals-0.6.0-dev.jar` — 461445 bytes.** SHA-256: `C7B13FDA8BFEB67096342EF84EA972F2723F59E0CEDFCC1AA36BBF837545C647`.

Это production/remapped мод; `nexvisuals-0.6.0-dev-sources.jar` не ставить в mods. Требуются Minecraft **1.21.11**, Java 21+, Fabric Loader >=0.19.5 и Fabric API >=0.141.6+1.21.11. Других обязательных библиотек нет. Sodium/Iris optional, устанавливаются отдельно. Предыдущий NexVisuals JAR заменить, не оставлять два экземпляра.

## Ручная проверка и ограничения

**Minecraft не запускался.** HUD/icons/animated reticle/pulses/Hearts/Pixels/picker не названы визуально проверенными. Не подтверждены FPS/GPU/runtime Mixin transformation, actual server play, resource reload и Sodium/Iris compatibility. Новые modules OFF по defaults, новые animation modes OFF/STATIC; прежний config сохраняет старые значения. Старые night Skybox, post shaders, wallpapers, Console Menu, mini HUD/Fire/Shield не переписаны.

[Точный checklist 0.6: HUD Editor, данные/таймеры, HSV/RGB/alpha, real-time wallpaper, crosshair, pulse, particles, persistence и совместимость](hud-customization.md#проверка-вручную).

Отчёты: `build/reports/tests/test/index.html`, `build/reports/tests/clientTest/index.html`. Полноценный shader pack, hidden entity information, automation, fake damage/kill confirmation не добавлены.

<details>
<summary>История 0.5.0-dev и более ранних этапов: старые JAR/числа не относятся к текущей сборке</summary>

# Проверка 0.5.0-dev — 30 сентября 2026

## Автоматически проверено

Minecraft из `gradle.properties` и production `fabric.mod.json` — **1.21.11**, не 1.21.1. Стек не обновлялся: Java 21 (Oracle 21.0.9+7-LTS-338), Fabric Loader 0.19.5, Fabric API 0.141.6+1.21.11, Loom 1.14.10, Gradle Wrapper 9.2.1, official Mojang mappings 1.21.11. Все cache/TEMP направлены внутрь проекта; использованы доступные зависимости `--offline`.

Фактически выполнено из `E:\Projects\nexvisuals`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
$env:TEMP = Join-Path (Get-Location) '.tools\tmp'
$env:TMP = $env:TEMP
.\gradlew.bat test clientTest --offline --console=plain --warning-mode=all '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\gradlew.bat clean build --offline --console=plain --warning-mode=all '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\tools\validate_shaders.ps1 -Validator '.tools\glslang-16.6.0\bin\glslang.exe'
git diff --check
```

- **Clean build успешен за 30 секунд**, все 15 tasks выполнены, `remapJar` и `verifyModJar` прошли. Java compilation/Gradle deprecation warnings в финальной сборке отсутствуют.
- **112 tests: 83 core + 29 client; 0 failures/errors/skipped.** Добавлено 13 focused tests: GroundMotion jump/landing/ledge/teleport, distance-based bounded StepCadence, FPS-independent EnvironmentEnvelope с pause/reset/drying, distinct editable recipes восьми реальных модулей, config restart, profile save/rename/load/delete, legacy defaults/night preservation, malformed values/clamping/corrupt-profile backup, independent world-image requests без grading. Все прежние тесты сохранены, включая actual catalog и pinned Mixin bytecode contracts; последние теперь проверяют также два новых adapters.
- Отдельно **5 GLSL 330 pairs compile/link** и UBO reflection с настоящими Minecraft 1.21.11 imports. SkyConfig 176, StarConfig 48, VisualConfig **272**, BackgroundConfig 96, HighlightConfig 16 bytes. Это shader source/interface/layout checks, не GPU draw.
- `git diff --check` успешен; whitespace новых untracked source/docs проверен отдельно. Нет commit/push/tag/release.
- Production JAR проверяет exact Minecraft `=1.21.11`, client-only metadata, отсутствие server entrypoint, declared Mixin classes, шесть particle masks, десять GLSL sources, три bundled profiles, отсутствие bundled Minecraft/test classes.

Промежуточный `clientTest` compile один раз завершился ошибкой: в новом тесте DoubleSetting был вызван `set(0)` вместо `set(0.0)`. Ошибка исправлена; повтор suite и последующий clean build успешны. Core tests при той неудачной попытке проходили. Shader checks не падали.

## Готовый production JAR

**`E:\Projects\nexvisuals\build\libs\nexvisuals-0.5.0-dev.jar` — 416612 bytes.** Это production/remapped mod, не `-sources.jar` и не dev artifact. SHA-256: `E6B92E1D4A28B9C7DC2A0F274EE2E4580994EB5C990B57A5180E004552B68ECE`.

Нужны Minecraft 1.21.11, Java 21+, Fabric Loader >=0.19.5, Fabric API >=0.141.6+1.21.11. Других обязательных модов/библиотек нет. Sodium и Iris по желанию устанавливаются отдельно; они не вложены в NexVisuals. Уберите прежний NexVisuals JAR при замене.

Отчёты: `build/reports/tests/test/index.html`, `build/reports/tests/clientTest/index.html`.

## Что проверяет пользователь

**Minecraft/runClient/computer-use не запускались.** Визуальные эффекты, runtime Mixin transformation, GPU driver output, resource reload, FPS и совместимость Sodium/Iris этой сборки не подтверждены. Implemented Iris pack guard — защита, не доказательство общей совместимости. Включены восемь новых модулей; существующие night Skybox, menu, mini HUD/Fire/Shield сохранены в коде и regression tests, но требуют краткой ручной проверки вместе с новым JAR.

Конкретный [checklist для восьми модулей, config/profiles, limits и совместимости](new-visuals.md#ручной-checklist). Старый расширенный regression checklist сохранён ниже как история предыдущего этапа.

<details>
<summary>История проверки 0.4.0-dev — предыдущий этап, старые JAR/числа не относятся к текущей сборке</summary>

# Проверка 0.4.0-dev — 30 сентября 2026

## Что проверено автоматически

Окружение: Windows, Oracle JDK `21.0.9+7-LTS-338`, Gradle Wrapper `9.2.1`, Loom `1.14.10`. Minecraft из `gradle.properties` — **`1.21.11`**. Использовались уже доступные локальные зависимости (`--offline`), без запуска игры.

Последовательность проверок:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
$env:TEMP = Join-Path (Get-Location) '.tools\tmp'
$env:TMP = $env:TEMP

.\gradlew.bat test clientTest --offline --console=plain '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\gradlew.bat clean build --offline --console=plain --warning-mode=all '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Djava.io.tmpdir=E:/Projects/nexvisuals/.tools/tmp'
.\tools\validate_shaders.ps1 -Validator '.tools\glslang-16.6.0\bin\glslang.exe'
git diff --check
```

Команды выполняются из `E:\Projects\nexvisuals`. `.tools/tmp` создана локально; переменные окружения не менялись системно. Этот workaround нужен ограниченному окружению Codex: прежняя попытка remap в системном TEMP получала Access denied. На обычном компьютере достаточно `gradlew.bat clean build` с JDK 21.

Итог: **финальный clean build успешен за 30 секунд; 99 tests (76 core + 23 client), 0 failures, 0 errors, 0 skipped**. Все 15 Gradle tasks выполнены. Предыдущий clean build тоже проходил; после source review исправлен wallpaper layering в in-world GUI через native queued blit и повторён полный clean build. В финальной сборке нет предупреждений Java или Gradle deprecations. `git diff --check` проходит; trailing whitespace отдельно проверен и в новых untracked файлах. `build` включает `test`, `clientTest`, `check`, `remapJar`, `verifyModJar`. Добавлено 10 focused tests для four-phase cycle, exposure/highlights и реальных sky/live settings/config/profiles/fallback; расширены существующие catalog/round-trip tests. Все прежние tests сохранены.

GLSL: **5 vertex/fragment пар compile/link успешны**, с настоящими `dynamictransforms.glsl`, `projection.glsl`, `fog.glsl` из Minecraft 1.21.11: sky_dome, custom_stars, visual_grade, live_background, glow_extract. Reflection glslang подтверждает размеры custom UBO: SkyConfig 176, StarConfig 48, VisualConfig 176, BackgroundConfig 96, HighlightConfig 16 bytes. Это проверка синтаксиса, stage interfaces и layout, **не GPU draw**. Использован официальный Khronos glslang 16.6.0 в ignored `.tools`; release ZIP SHA-256: `82bf434e69b9bb4829de7e2b4bc2c5e7a7861e53d66cf75e5cc70f5f694a8d9b`. Скрипт сам не скачивает инструменты; на другом компьютере передайте `-Validator` и `-MinecraftJar` по фактическим путям.

Промежуточные проверки этого этапа также выявляли ошибки: compileClientJava остановился из-за package-private ProfilesScreen и пропущенного Std140Builder import; compileClientTestJava — из-за int вместо Double в новом тесте. Исправлены public visibility экрана, import и значение 0.0 в тесте. После исправлений полный suite и GLSL checks проходят. Ошибки прошлого этапа 0.3.0 (Math/noise3/glow ID и две celestial constants) уже были исправлены в исходном baseline.

| Проверка | Покрытие |
| --- | --- |
| Core settings/module tests | IDs, duplicate IDs, lifecycle, validation, clamping, JSON types |
| Config/profile tests | Round-trip, defaults, missing/unknown fields, broken JSON/UTF-8 backups, persistence, unsafe paths, rename without overwrite |
| Animation/math tests | FPS-independent transitions, easing, bounded envelope/color/arc, visual swing timeline |
| TransferMatcher tests | Exact single/split-stack conservation; no destination for ambiguous/asynchronous changes |
| HUD positions | Normalized drag, viewport clamping, resized windows, oversized elements |
| Mini HUD regression | Общий bottom-center и spacing на чётной/нечётной ширине; inheritance без двойного scale; старый config; Link OFF; group drag/resize |
| New cosmetic math | Симметрия elytra emitters, teleport/idle rejection, лимит samples и равномерный Fireflies spawn без backlog |
| Console Menu motion | Различные траектории, reverse, bounded sway/tilt, yaw wrap, staggered entrance; clock при 20/30/60/144 FPS, pause/zero-speed/stall |
| Console Menu layout | Demo/normal/development layouts, Left/Center/Right, размеры от 320×240 до 1920×1080 GUI pixels; logo, buttons, footer без overlap |
| Console Menu persistence | Цвета с alpha, movement/reduced-motion/loading/enabled в config и named profile; старый config остаётся opt-in; invalid enum/default/clamping; reset после Still |
| Sky/post/live settings и profiles | 9 sky / 8 post / 6 live recipes, group coverage, distinct configs, Current/Custom, config + profile round-trip, ARGB/clamping/defaults; старые ночные presets не включают новые слои |
| Live fallback и old config | Vanilla mode/цвета независимы от Console layout, сохранение и reload; отсутствие нового module leaves it OFF; invalid style изолирован, shape/motion parameters clamp-ятся |
| Sky math / star field | Плавные нормализованные периодические 3- и 4-phase weights; sunrise ≠ sunset, exact night palette, brightness bounds; fog только усиливается; deterministic stars, cap 4000, bounded twinkle |
| Color grading reference | Identity/grayscale/warm-cold, exposure в stops, selective highlights, black при новых extremes и bounded outputs; CPU reference, не GPU image |
| GLSL compile/link | Five GLSL 330 programs с imports фактического 1.21.11 и UBO sizes |
| Crosshair geometry | Различные Circle/Chevron masks, ограниченные размеры, открытый центр кольца и отсутствие overlap у alpha fill/outline |
| Client catalog | Actual registration list, unique settings, all disabled by default, every built-in preset, bundled global recipes, save/load of real modules, Copy/Mirror, old foundation config |
| Mixin bytecode contracts | Target methods, captured argument types, shadow fields, INVOKE sites и обе celestial size constants в pinned game classes |
| Production JAR | Exact Minecraft `=1.21.11`, client environment, no server entrypoint, все mixin classes, five particle PNGs и ten GLSL sources, no bundled game/test classes |

Отчёты: `build/reports/tests/test/index.html` и `build/reports/tests/clientTest/index.html`. Production/remapped JAR: **`E:\Projects\nexvisuals\build\libs\nexvisuals-0.4.0-dev.jar` — 366625 bytes**. `-sources.jar` не предназначен для установки. Проверено содержимое реального JAR: `version: 0.4.0-dev`, `minecraft: =1.21.11`, Java >=21, Fabric Loader >=0.19.5, Fabric API >=0.141.6+1.21.11; других обязательных библиотек нет. Sodium/Iris устанавливаются пользователем отдельно по желанию, не включены в мод.

## Что не проверено

**Для этого этапа не запускались `runClient`, Minecraft, computer-use или интерактивные GUI-тесты.** Пользователь подтвердил предыдущий sky/post baseline и отдельно удачный ночной skybox. Здесь сохранены старые night recipes и добавлены dynamic day, live wallpaper и расширенные world-image passes. Console Menu менялся только для совместного background rendering. Feedback предыдущей версии не подтверждает GPU/runtime этой сборки.

Не проверены GPU output, звук, FPS под нагрузкой, actual Mixin transformation в запущенном клиенте, multiplayer prediction, resource reload, Sodium/Iris совместимость. Старые `run/logs` не являются логами новой сборки. Mixin contract tests и успешный remap снижают риск неверных сигнатур, но не заменяют runtime.

## Ручной checklist

Приоритет после обновления 0.4.0:

- **Dynamic/daytime:** World → Custom Skybox → Dynamic / NexVisuals → Apply → Enabled. Оценить morning/noon/sunset/midnight и непрерывные переходы, haze, sun halo и cycle fog. Enhanced Day — более спокойный вариант. В singleplayer test world можно сравнить `/time set 0`, `6000`, `12000`, `18000`; команды зависят от разрешений пользователя. Оценить восход/закат и при обычном течении времени, а не только snapshots. Старые Deep Night/Purple Nebula должны сохранить прежний ночной стиль.
- **Live Background:** Interface → Live Background → Enabled, Background: Live. Сравнить все 6 presets на vanilla title, Console Menu и прямо за редактором/Profiles; одинаковую ориентацию изображений между direct title и queued canvas. Проверить три цвета/alpha, speed 0, motion 0, intensity/brightness/saturation/softness/motes/dim, Reduced motion из Console Menu. Resize/GUI scale, contrast текста, кнопки/mouse/Tab должны остаться обычными. Optional Pause Menu ON/OFF; wallpaper должен перекрывать HUD, но не controls; после закрытия HUD возвращается обычно. Контейнеры/загрузка не заменяются. Background: Vanilla и module OFF возвращают прежний фон; кнопка Console Vanilla меняет layout отдельно. Проверить отсутствие двойного panorama overlay, вспышки vanilla при открытии и артефактов F3+T.
- **Новые effects:** Dreamy/Cinematic/Vibrant/Retro — различия halo, edge softness, grain size, highlights и цветов. Wide/Compact/OFF на ярком небе, torch/high-contrast surfaces; без глобальной засветки, ghost copies и чёрных краёв. Exposure/highlights bounds, sharp center/HUD/text. Resize/reload, сравнение FPS и RAM/VRAM, возвращение к миру после длинного пребывания в меню. GPU визуально автоматически не проверялся.

- **Skybox:** сравнить все 9 presets; horizon height/softness, palette/brightness/saturation/tint и day/night influence 0/1. Проверить rain, нахождение ниже sea horizon, водоём, lava, Blindness/Darkness и Nether/End. OFF возвращает обычное небо; если Sky Palette включён, он продолжает работать отдельно.
- **Stars:** Custom / Vanilla / Disabled; Pixel/Diamond/Soft, amount 0/600/4000, size, brightness, ARGB opacity и twinkle speed/intensity. В Vanilla amount/size/shape намеренно не меняют Minecraft geometry. Повороты камеры не должны двигать звёзды вместе с экраном; сохраняется реальный суточный celestial rotation.
- **Sun/Moon:** size/opacity/tint, Blood Moon и фазы луны; движение и moon phase должны быть vanilla. Проверить resource pack, FOV, resize и F3+T reload.
- **Atmosphere/Fog:** отдельно Aurora, Nebula, Shooting stars (подождать выбранный interval), Horizon glow; intensity/colors/motion. Fog OFF/ON, blend/alpha, density 1–3 — плотность только увеличивается. Сравнить FPS с nebula/aurora ON/OFF и полем 4000 stars.
- **Post Processing:** все 8 presets, отдельные grading/vignette/glow/chromatic/grain/filter/night/damage toggles и overall intensity 0/1. Проверить full-window output без переворота, чёрных краёв, мерцания или изменения HUD/text. OFF возвращает vanilla world image; Screen Tint остаётся отдельным модулем. Проверить воду, повреждение игрока, spectator entity post effect, resize/F3+T, fast/fancy/fabulous graphics.
- **GUI/config:** Sections, scrolling, Current → Custom после edits, reset всего модуля при открытом разделе, Preview/F4/Esc: обои скрываются, виден мир, clicks/typing не проходят к gameplay/скрытым controls. Save named profile, reset/load, restart: live enabled/style/colors/motion, sky preset и новые post параметры должны восстановиться; старый config не включает новые модули автоматически.
- **Sodium/Iris:** повторить sky/post/live проверки с Sodium именно для 1.21.11. Iris без pack: проверить новые effects; с pack: sky/post должны показать `Paused` и уступить pack, меню с Live Background проверяется отдельно. Pack OFF — возвращение NexVisuals без crash/black frame. API failure — safe pause. Записать фактические версии и проверить `latest.log`: mixin/shader/draw errors не должны игнорироваться. Ни одна из этих runtime комбинаций автоматически не проверялась.

Регрессии Console Menu из 0.2.2:

- **Включение/выключение:** «Меню NexVisuals» слева сверху vanilla title screen → новое меню. «Стиль меню...» открывает Console Menu; Done/Esc возвращает к нему. Vanilla возвращает обычное оформление. После restart восстанавливается выбранное состояние.
- **Стили и движение:** Classic/Sunset/Moonlight/Still, все 4 motion types, speed/zero/reverse/amplitude/tilt, цвета/alpha, Left/Center/Right, Reduced motion, Minecraft Panorama Scroll Speed = 0. Проверить читаемость текста, анимацию входа/hover и сохранение custom values после restart.
- **Действия меню:** Singleplayer, Multiplayer, Realms, Options, language/accessibility, Quit. Проверить keyboard Tab/Enter, mouse hit areas во время entrance, disabled tooltips при ограничениях аккаунта, маленькое окно/GUI Scale и возврат после игры. Автоматический layout test проверяет геометрию, а не GPU output или клики.
- **Загрузка:** обычный вход в мир/на сервер, World loading theme OFF, реальный chunk/progress display, переходы Nether/End. Проверить отсутствие перекрытий loading footer и chunk map при своей дальности прорисовки.
- **Профили:** сохранить custom menu, изменить его, загрузить профиль; Reset settings; восстановить vanilla. Общие Clean/Aurora/Cinematic recipes сбрасывают неуказанные модули, в том числе выключают Console Menu — это существующая семантика полного профиля.

Регрессии предыдущего обновления:

- **Mini HUD:** HUD → Hotbar / Mini HUD → Mini → Apply → Enabled. Убедиться, что hotbar, hearts/armor/hunger, XP и offhand центрированы и уменьшаются вместе. Проверить underwater air, mount health, item name, GUI scale и resize. Link OFF должен вернуть индивидуальные настройки; HUD Editor перемещает связанную группу целиком. Ничего не нужно удалять из config.
- **Keystrokes:** проверить плитки, подсветку, переназначенные клавиши, resize/drag и `Key tiles OFF`.
- **Fireflies:** Particles → Fireflies → Meadow вечером под открытым небом, Embers в любое время; сравнить цвета/радиус/size/amount. Дать несколько секунд на заполнение. Проверить отключение, Minimal particles, смену мира и глубину у стены.
- **Elytra Trails:** Particles → Elytra Trails → Aurora/Comet/Halo, F5 и настоящий полёт на элитрах. Проверить симметрию, повороты, посадку, teleport, выключение и сохранение настроек после restart. Крылья эмиттера приближённые, без привязки к костям модели.

1. Установить production JAR в отдельный тестовый профиль **Minecraft 1.21.11**, Fabric Loader 0.19.5+, Fabric API 0.141.6+1.21.11 и Java 21. Убрать предыдущий JAR NexVisuals. Проверить отсутствие crash/mixin errors в `latest.log`.
2. Зайти в мир. Открыть **Right Shift** или NexVisuals в pause menu; изменить keybind через Controls. Проверить категории, поиск, scrolling, preset selector/Apply, colors/alpha/swatches, reset и маленький размер окна. Console Menu оформляет главное меню отдельно и только при включении.
3. **Hit Effects:** по видимой сущности сравнить Burst/Sparks/Rings/Slash/Impact, затем size/count/speed/gravity/lifetime/fade/opacity и accents. Проверить стены/глубину, отсутствие missing-texture клеток, быструю серию атак и Particle settings Minimal/Decreased. Эффект на отклонённой сервером атаке допустим: это local attempt.
4. **Viewmodel:** main/offhand, left-handed mode, Copy/Mirror, per-axis scale, reset. Сравнить Compact/Centered/PvP/Cinematic, sword, empty hand, map, bow, crossbow, eating и смену предмета.
5. **Swing:** все семь вариантов, duration/amplitude/return balance, Custom translations/rotations/scale. Проверить начало и возврат в neutral, частые клики и неизменный vanilla attack cooldown. Специальные map/bow/spear paths могут остаться vanilla.
6. **Fire:** Vanilla/Low/Minimal/Hidden, alpha и position. Реальное burning/damage должно остаться. **Shield:** resting/blocking, обе руки, все presets; blocking по-прежнему работает как vanilla.
7. **Trails:** третье лицо, движение/прыжок, остановка, teleport, смена мира и отключение. Сравнить Silk/Sparks/Rings/Rainbow, проверить lifetime и нагрузку. **Hat:** F5, поворот головы, crouch/swim, helmet, radius/height/gradient/rotation, стена между камерой и игроком; шляпа не должна появляться сквозь препятствия или на скрытых игроках.
8. **Sounds:** Preview и атаки для всех 6 вариантов, volume/pitch/variation, Master/UI sound volume, resource pack. Ничего не должно звучать при выключенном модуле.
9. **Sky:** Overworld palettes, sunrise/night/rain, Custom colors, blend 0 и выключение; terrain lighting/time/fog должны остаться прежними. Nether/End обычные. С Iris палитра должна быть неактивна; General → Open Iris shader settings проверяется отдельно. Shader-pack presets NexVisuals не предоставляет.
10. **HUD:** drag/resize/restart positions, H toolbar, toggles, scale и reset для text и vanilla HUD. Проверить survival hearts/armor/hunger, дополнительные hearts/absorption, effects, boss bars, selected-slot outline и offhand. Experience/info layer может показывать locator/jump bar согласно обычному Minecraft.
11. **Containers:** Inventory, chest, crafting table, furnace/blast furnace/smoker, anvil, enchanting, brewing, shulker, hopper. Проверить hover, Left/Right Click, Shift+Click, drag, pickup/drop. Сравнить Slide/Smooth/Arc/Pop/Fade, split-stack transfers и полную инвентарную сетку. Slots должны оставаться кликабельными по обычным координатам, количество предметов не должно меняться из-за эффекта. Неопределённый destination → только pulse, без маршрута.
12. **Profiles/persistence:** Save `My PvP`, изменить несколько модулей, Load; создать второй профиль, Rename, Delete с подтверждением, Defaults. Сравнить Global Styles Clean/Aurora/Cinematic и восстановить свой сохранённый профиль. Проверить Crosshair Classic/Dot/Wide/Precision/Orbit/Chevron. Закрыть и перезапустить игру, проверить enabled/settings/positions. Проверить также выключение всех модулей: визуальное поведение возвращается к vanilla после истечения/очистки частиц.
13. Повторить подходящие пункты с конкретными версиями Sodium/Iris, если они используются, и проверить `latest.log`. Пока их совместимость **не заявляется**.

## Известные ограничения

- Dynamic sky / Live Background / новые post effects визуально не проверены; successful GLSL link и bytecode contracts не гарантируют правильный GPU output или actual Mixin transformation. Совместимость с Sodium/Iris требует ручного подтверждения.
- Skybox процедурный и применяется только к Overworld air; внешние cubemaps, отдельная замена vanilla clouds/End sky и shaders внутри Iris pack не реализованы. Устанавливая Iris с pack, пользователь выбирает rendering pack вместо новых NexVisuals passes.
- Glow остаётся LDR image-space эффектом: Compact — четыре samples, Wide — quarter-resolution luminance extraction и девять reconstruction samples. Нет HDR, depth-aware blur, shadows/SSR/DOF/volumetrics или private shader pack options. Highlight из уже обрезанного vanilla LDR не восстанавливает HDR lighting.
- Live Background не заменяет все vanilla screens и не поддерживает внешние videos/images; loading/containers остаются прежними. Softness — мягкие формы, а не real-time blur. Разные GUI overlays дают немного разное итоговое dim. Motes approximate; эффект зависит от GPU/разрешения, FPS не измерен.
- Не проверены title-screen replacements и дополнительные кнопки других модов. Они могут требовать отдельного layout adapter. Realms notices остаются под управлением vanilla.
- Console Menu не заменяет начальную заставку Mojang при resource loading и не добавляет controller support. Nether/End portal backgrounds сохраняются. Это тема Java Edition, не порт Xbox 360 UI.
- Generic GUI остаётся рабочим редактором, а не окончательным редизайном по референсам. Не весь текст локализован; интерфейс преимущественно английский.
- Vanilla HUD bounds приблизительные. Universal sprite alpha/tint, heart spacing и переписывание стандартных HUD textures отложены.
- Container animation не перемещает интерактивные slots и не задерживает закрытие. Ghost alpha заменён shrink-out; полная destination animation есть только для однозначного QUICK_MOVE.
- Shield opacity, непрерывные mesh ribbons, пользовательские skybox assets, recipes для сторонних shader packs, полноценный curve editor и tooltips reskin отложены. Собственные lightweight presets уже реализованы.
- Other crosshair replacement mods и modded containers, обходящие `AbstractContainerScreen`/`slotClicked`, отдельно не поддержаны/не проверены. Сохранение vanilla attack indicator использует геометрию vanilla reticle именно 1.21.11.
- Есть только клиентская косметика локального игрока; синхронизации hats/trails другим игрокам нет.

</details>

</details>
