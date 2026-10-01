# Item Swing и Weapon Trails — 0.8.2

Minecraft **1.21.11**, Java 21, прежний Fabric stack. Это исправление существующих модулей; их ID и прежние настройки сохранены.

## Item Swing

Откройте **Viewmodel → Item Swing**, включите модуль, выберите preset и нажмите **Apply**. Прежние вручную настроенные значения не заменяются автоматически. Apply даёт новые сбалансированные duration/peak/easing; затем их можно редактировать.

| Preset | Движение | Visual duration |
| --- | --- | --- |
| VANILLA | Штатный swing Minecraft | Штатная |
| SMOOTH | Мягкий наклон с небольшим подъёмом/движением в глубину | 300 ms |
| SWIPE | Быстрый боковой разрез, дуга по высоте, возврат | 260 ms |
| SLASH | Выраженный диагональный разрез вниз | 320 ms |
| SPIN | Один полный оборот вокруг оси предмета | 420 ms |
| PUSH | Короткое движение вперёд и небольшое расширение | 240 ms |
| CUSTOM | Собственные translation X/Y/Z, pitch/yaw/roll и peak scale | 300 ms по умолчанию |

Во всех custom modes доступны Amplitude, Attack / return balance и Easing. Visual duration **не меняет** реальную скорость атаки, cooldown, reach или packets. Main/off hand отслеживаются независимо; левая рука зеркалит движение.

Раньше начало цикла выводилось из интерполированного progress во время render. Теперь короткий `LocalPlayerSwingMixin` наблюдает результат штатного `LocalPlayer.swing`: новый цикл принят vanilla, только когда `swinging`, `swingTime == -1` и нужная `swingingArm`. Одиночный взмах в воздух, атака сущности и взмах при добыче блока используют один путь. Отвергнутые vanilla попытки продолжить mining не перезапускают визуальный clock; повтор в том же tick также не сбрасывает его. Время монотонное, рендер только читает progress.

Повторный принятый swing переносит текущую позу и плавно отпускает её за 65 ms, вместо скачка к нейтральному положению. В конце анимация возвращается к статическому Viewmodel. На idle не выполняются лишние pose rotations. World/player/item/main-arm/style changes, переход в third person и использование предмета сбрасывают контекст. Bow/eating/brush, maps, empty hand, auto-spin и spear STAB сохраняют штатные пути. Это обычный first-person WHACK, не переопределение всех возможных item animations или сторонних combat mods.

## Weapon Trails

**Viewmodel → Weapon Trails → Prism / Ember / Frost → Apply**. Модуль выключен по умолчанию. Сохраняются два ARGB цвета, lifetime и Include off hand.

- **Blade alignment: AUTO** по умолчанию использует actual item display transform. Если старые base/tip probes были вручную изменены, AUTO оставляет прежнее hand-space выравнивание.
- **MODEL** всегда использует transform первого item layer, включая resource pack, Viewmodel/Swing и left-hand transform. Probes проходят по верхней диагонали обычной sword/axe модели. **Blade coverage** меняет длину этой части (15–100%); это косметическая линия, не автоматическое распознавание границы текстуры.
- **LEGACY_HAND** и шесть прежних base/tip X/Y/Z позволяют откалибровать необычную модель вручную. Reset probes или Apply preset возвращает AUTO/model mode.
- **Smoothness 1–3** добавляет ограниченную интерполяцию между samples. Новые default 2; позиции не выходят за локальные границы соседних probes.
- **Lifetime 40–350 ms**, presets: Prism 150 ms, Ember 110 ms, Frost 190 ms. Цвет/alpha затухают по возрасту, наружные края прозрачны.

Каждый принятый local swing начинает отдельную историю. Ленты разных ударов не соединяются. Item/world/setting changes, разрыв рендера более 100 ms, скачок probe более 0.8 render-space units и приближение за near plane разрывают историю. На idle новые точки не записываются; оставшиеся исчезают по lifetime. Использование предмета, spectator/invisibility, third person и остальные предметы исключены.

`ItemInHandRendererMixin` оборачивает существующий `renderItem`, всегда вызывая original, и очищает scope в `finally`. Новый `ItemModelTrailMixin` пассивно берёт pose после штатного `ItemTransform.apply` только внутри этого scope, один раз на rendered item. Никакой повторной загрузки/разбора моделей или textures нет. Mesh в camera space идёт в существующую Minecraft submit queue с identity pose, `RenderTypes.debugQuads`, alpha/depth и sorted quads. Собственных GPU resources, shaders/framebuffers нет.

Лимиты: максимум **48 пар probes на руку**, sampling не чаще **100 Hz**, lifetime максимум 350 ms. При Smoothness 3 верхняя граница — **423 quads / примерно 27 KiB vertex/color arrays на руку за snapshot**; defaults — 282 quads. Native queue требует неизменяемый snapshot; нет объектов на каждый vertex, бесконечных collections или disk I/O в render. Это расчётные пределы, не FPS benchmark.

Прежний Iris active-pack guard сохранён: trails приостанавливаются при включённом pack или недоступном публичном API и показывают причину в GUI. Sodium/Iris runtime compatibility в этом этапе не проверялась.

## Автоматическая и ручная проверка

Чистые тесты проверяют clocks, duplicate ticks, независимость рук/FPS, все pose styles, carry/neutral endpoints, rate/capacity/expiry/near-plane guards и ограниченную геометрию. Headless tests проверяют реальные module poses от одного события, presets/config/profiles/legacy probes. Mixin contracts сверяют targets, captured descriptors, wrapped receiver/args и invocation sites по фактическому Minecraft 1.21.11. Это не live Mixin transformation и не визуальная проверка.

Minecraft не запускался. Пользователю проверить:

1. Каждый Item Swing preset: один короткий LMB в воздухе с мечом/топором, атака видимой сущности, быстрые повторные клики, удержание LMB при добыче блока. Предмет двигается и сам возвращается; скорость реальной атаки прежняя.
2. CUSTOM: offsets/rotation/scale, duration/easing/amplitude; static Viewmodel и обе руки. OFF/VANILLA возвращают native swing. Bow/eating/maps/spear/third person остаются штатными.
3. Weapon Trails: MODEL/AUTO, Coverage и Smoothness, все палитры, left main arm/offhand, быстрые повторные swing. Нет перемычек между ударами, idle след исчезает, смена предмета и OFF очищают его.
4. При необычном resource pack откалибровать LEGACY_HAND probes. Проверить near-plane offsets, маленький FPS и отсутствие больших sheets перед камерой.
5. Reset, Profiles/save/load, restart и сохранение старых настроек. Проверить Sodium и Iris с pack/без pack отдельно.

Готовый remapped JAR и результаты итогового build — в [validation](validation.md). Не устанавливайте sources JAR и не оставляйте старый NexVisuals JAR рядом с новым.
