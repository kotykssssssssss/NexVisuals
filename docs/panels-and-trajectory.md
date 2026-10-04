# Панели категорий и Local Trajectory — 1.1.0-dev

Minecraft Java Edition **1.21.11**, Fabric, Java 21+. Изменяется текущий проект; прежние modules/settings/config/presets/rendering systems продолжают использоваться.

## Интерфейс

Вместо sidebar и общей вертикальной ленты интерфейс показывает отдельные компактные окна категорий над миром. Направление референса использовано для композиции окон; функциональность, цвета и стилистика остаются NexVisuals. Cheat-категории/модули из референса не добавлены.

- **Right Shift** открывает меню в мире; привязка остаётся в Minecraft Controls.
- Заголовок окна: перетаскивание перемещает, короткий ЛКМ / ПКМ сворачивает. Tab и Enter позволяют управлять заголовком с клавиатуры.
- ЛКМ по строке модуля включает/выключает его; ПКМ, `>` справа или Right Arrow при фокусе открывает настройки. Space/Enter переключают модуль.
- Колесо прокручивает отдельное окно под курсором. Скрытые/обрезанные строки не принимают клики.
- Поиск фильтрует **все** категории по name/ID/description; Ctrl+F ставит фокус в поле. Изменения курсора без изменения текста не пересоздают widgets, не сбрасывают scroll и не создают tooltip заново.
- Общий редактор содержит прежние sections, sliders, boolean/enum/text/color/keybind controls, presets/actions/reset и runtime status. Esc или клик вне редактора закрывает его.
- **Profiles**, **HUD editor**, **General**, **Preview/F4** и существующий Live Background сохранены. Preview скрывает интерфейс, но не передаёт игровое управление.
- **Arrange** возвращает grid, раскрывает окна и сбрасывает прокрутку. **Panels N/M** переключает страницы категорий при высоком GUI Scale / маленьком окне.
- General → **Remember interface layout** сохраняет позиции/свёрнутость при закрытии меню. Координаты нормализованы в доступной области: при resize окна остаются внутри экрана. Порядок наложения сохраняется во время работы текущего экрана; поиск, страницы и scroll не сохраняются на диск.

Все существующие модули появляются через `ModuleRegistry` / `Category`: Viewmodel, Swing, Weapon/Player/Projectile Trails, Hit Effects, HUD, Skybox, Post Processing, Live Background, Console Menu, sounds и другие. Специальных screens для каждого модуля нет. Добавление нового зарегистрированного модуля автоматически создаёт его строку и настройки.

## Собственная траектория

**World → Local Trajectory**, ID `local_trajectory`, по умолчанию OFF. Модуль оценивает бросок/выстрел **только из текущего local player** в first person, пока поддерживаемый предмет удерживается или заряжается. После выпуска снаряд не отслеживается. Никаких foreign projectile hooks, entity scans, информации о целях, raycast по сущностям или изменения packets/gameplay нет.

| Предмет | Условия / модель |
| --- | --- |
| Bow | Во время натяжения, есть vanilla arrow ammunition; сила vanilla charge curve, скорость до 3 blocks/tick |
| Crossbow | Уже заряжен ровно одной arrow/spectral/tipped arrow, 3.15 blocks/tick; не наследует движение стрелка |
| Ender pearl, Snowball, Egg / Blue Egg / Brown Egg | 1.5 blocks/tick, gravity 0.03 |
| Splash / Lingering Potion | 0.5 blocks/tick, pitch offset −20°, gravity 0.05 |
| Trident | Натяжение >=10 ticks, не Riptide, не разрушается при следующем использовании; 2.5 blocks/tick |

Для остальных `shootFromRotation` вариантов наследуются X/Z известного local movement и Y при нахождении в воздухе. Начало — позиция игрока / eyeY−0.1F; yaw/pitch используют Minecraft float sin/cos. Зажатый use item имеет приоритет над main hand; off hand берётся при пустой main hand. Модель намеренно не угадывает, будет ли правый клик использован блоком вместо броска.

Физика проверена относительно **native 1.21.11**: arrow/trident сначала перемещаются, затем применяют air drag 0.99F / gravity 0.05. Throwables сначала применяют gravity и drag, затем перемещаются. Native block clip учитывает collision shapes, а не только полные кубы. Маркер — кольцо на нормали первого известного блока; нет ложной отметки на конце лимита/неизвестного участка.

**Внешний вид:** Line / Points / Line+Points, ARGB color + opacity, world-space thickness, fade, impact marker / radius. Встроенные recipes **Azure Guide**, **Minimal**, **Pearl Dots** используют разные style/width/opacity/time/color. Можно менять каждую настройку, сбрасывать отдельную/весь модуль и сохранять конфигурацию в обычные profiles.

## Ограничения

- Это **приблизительная центральная траектория**, не обещание точного попадания: server RNG spread не известен, сущности не проверяются и могут перехватить снаряд до блока. Серверные plugins / изменённая физика / рассинхронизация также могут менять результат.
- Multishot и fireworks в crossbow, неизвестная bow ammo, Riptide и другие неподдерживаемые предметы не симулируются. В транспорте, spectator и third person preview не рисуется.
- Модель прекращается при жидкости, portal/gateway, cobweb, powder snow/scaffolding, границе мира/build height и незагруженных chunks. Для chunk safety сегмент проверяет ограничивающий набор chunks; на границе диагонали это может консервативно остановить путь раньше.
- Предположение collider не содержит entity-specific collision context; проблемные powder snow/scaffolding исключены. Сложные block states / datapacks требуют ручной проверки.
- Активный Iris shader pack или недоступный публичный Iris API **приостанавливает Trajectory** через прежний compatibility guard с сообщением в настройках. Практическая совместимость с Sodium/Iris не проверена.

## Архитектура и производительность

`CategoryBoardLayout` рассчитывает grid / pagination / inspector. `CategoryPanel` управляет bounds, drag, fold и `ScrollPane`. `ModuleRow` — общий native Button с toggle/settings поведением и accessibility narration. `NexVisualsScreen` лишь соединяет их с прежним `SettingControls`. Позиции представлены `core.ui.PanelPosition` и сериализуются в необязательное `gui.panels.<CATEGORY>` (`x`, `y`, `collapsed`). Схема config **1** сохранена: отсутствующие поля используют defaults, повреждённый layout одной категории не уничтожает соседние; profiles используют тот же serializer.

`TrajectoryMath` — независимый Java интегратор с переиспользуемым primitive scratch buffer и trace result. `TrajectoryModule` выбирает только local held item и выполняет ограниченные проверки загруженных chunks, native block clipping и vanilla voxel traversal для неподдерживаемых условий. Пересчёт — максимум один раз за game tick; immutable primitive snapshot передаётся Fabric **END_EXTRACTION → AFTER_ENTITIES**. Drawing не читает мир / inventory / config, использует штатный `RenderTypes.debugQuads` с обычной глубиной: ribbon, point quads и 24-segment impact ring. Предыдущий nullable world context crash учитывается: оба callbacks проверяют подготовленность state, extraction очищает stale frame.

Hard caps: **120 ticks / 160 blocks** пройденного пути, максимум **121 point**, 8 blocks на шаг (иначе остановка). Нет истории снарядов, бесконечных collections, собственного framebuffer/shader pipeline, новых Mixins или сторонних модов. При OFF callbacks сразу возвращаются; paused client не пересчитывает prediction. Vanilla clipping создаёт небольшие объекты во время ограниченного tick simulation, не каждый draw frame. Point billboards учитывают все три оси направления к камере; вертикальный бросок не вырождает их геометрию. Это отдельно проверяется headless geometry test.

## Проверить вручную

1. Right Shift в мире; ЛКМ toggle, ПКМ / `>` settings, sliders, enum/section/preset popup, color picker, keybind capture, reset. Проверить прежние Viewmodel / Swing / Effects / HUD / Skybox / sounds.
2. Drag/fold/overlapping panels, колесо внутри разных окон, поиск + длинный список + descriptions, Tab/Space/Enter/Right Arrow. `Arrange`, маленькое окно и высокий GUI Scale → доступность всех страниц.
3. Bow: разное натяжение/углы, движение/прыжок. Crossbow: одна arrow; Multishot / firework не должны показывать ложную траекторию. Pearl/snowball/разные eggs, potions и non-Riptide trident.
4. Сравнить реальный выстрел с примерной линией и первым block marker на стене, slab/stairs/fence; проверить Line/Points, fade, цвета/размер. Около entities линия остаётся block-only estimate и **не** обозначает попадание в цель.
5. Жидкость/портал/граница chunks, unsupported item/cooldown, OFF, third person, смена мира: без зависшего пути/ложного marker. Предметы других игроков не создают prediction.
6. Profiles save/load, GUI placement/fold и Trajectory settings после restart; old config продолжает работать.
7. FPS/performance на своей системе; Sodium и Iris отдельно. При active Iris pack ожидается объяснённая приостановка Trajectory.

Minecraft/runClient/computer-use в этом проходе **не запускались**. Автоматические результаты и production JAR: [validation](validation.md).
