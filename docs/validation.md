# Проверка 0.2.2-dev — 30 сентября 2026

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
git diff --check
```

Команды выполняются из `E:\Projects\nexvisuals`. `.tools/tmp` создана локально; переменные окружения не менялись системно. Этот workaround нужен ограниченному окружению Codex: прежняя попытка remap в системном TEMP получала Access denied. На обычном компьютере достаточно `gradlew.bat clean build` с JDK 21.

Итог: **clean build успешен; 77 tests, 0 failures, 0 errors, 0 skipped**. В финальной сборке нет предупреждений Java или Gradle deprecations. `git diff --check` проходит. `build` включает `test`, `clientTest`, `check`, `remapJar`, `verifyModJar`. Добавлены 12 проверок motion/clock/layout и реального модуля Console Menu; остальные regression tests сохранены.

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
| Crosshair geometry | Различные Circle/Chevron masks, ограниченные размеры, открытый центр кольца и отсутствие overlap у alpha fill/outline |
| Client catalog | Actual registration list, unique settings, all disabled by default, every built-in preset, bundled global recipes, save/load of real modules, Copy/Mirror, old foundation config |
| Mixin bytecode contracts | Target methods, captured argument types, shadow fields, INVOKE sites in pinned game classes |
| Production JAR | Exact Minecraft `=1.21.11`, client environment, no server entrypoint, all mixin classes and five particle PNGs, no bundled game/test classes |

Отчёты: `build/reports/tests/test/index.html` и `build/reports/tests/clientTest/index.html`. Production/remapped JAR: **`E:\Projects\nexvisuals\build\libs\nexvisuals-0.2.2-dev.jar` — 295066 bytes**. `-sources.jar` не предназначен для установки.

## Что не проверено

**Для этого этапа не запускались `runClient`, Minecraft, computer-use или интерактивные GUI-тесты.** Пользователь прямо оставил runtime/visual testing за собой и сообщил, что предыдущие функции работают. Mini HUD, shield и fire в этом обновлении не менялись. Это не подтверждает runtime нового Console Menu.

Не проверены GPU output, звук, FPS под нагрузкой, actual Mixin transformation в запущенном клиенте, multiplayer prediction, resource reload, Sodium/Iris совместимость. Старые `run/logs` не являются логами новой сборки. Mixin contract tests и успешный remap снижают риск неверных сигнатур, но не заменяют runtime.

## Ручной checklist

Приоритет после обновления 0.2.2:

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

- Новое Console Menu визуально не проверено; ссылки на supported APIs не являются гарантией отсутствия графических артефактов.
- Не проверены title-screen replacements и дополнительные кнопки других модов. Они могут требовать отдельного layout adapter. Realms notices остаются под управлением vanilla.
- Console Menu не заменяет начальную заставку Mojang при resource loading и не добавляет controller support. Nether/End portal backgrounds сохраняются. Это тема Java Edition, не порт Xbox 360 UI.
- Generic GUI остаётся рабочим редактором, а не окончательным редизайном по референсам. Не весь текст локализован; интерфейс преимущественно английский.
- Vanilla HUD bounds приблизительные. Universal sprite alpha/tint, heart spacing и переписывание стандартных HUD textures отложены.
- Container animation не перемещает интерактивные slots и не задерживает закрытие. Ghost alpha заменён shrink-out; полная destination animation есть только для однозначного QUICK_MOVE.
- Shield opacity, непрерывные mesh ribbons, пользовательские skybox assets, шейдерные preset recipes, полноценный curve editor и tooltips reskin отложены.
- Other crosshair replacement mods и modded containers, обходящие `AbstractContainerScreen`/`slotClicked`, отдельно не поддержаны/не проверены. Сохранение vanilla attack indicator использует геометрию vanilla reticle именно 1.21.11.
- Есть только клиентская косметика локального игрока; синхронизации hats/trails другим игрокам нет.
