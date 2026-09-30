# Возможности 0.2.2-dev

Все модули выключены по умолчанию. В мире откройте меню **Right Shift** (переназначение через Minecraft Controls) или кнопкой NexVisuals в pause menu. Выберите модуль, включите его, выберите встроенный стиль кнопкой `>` и нажмите `Apply`. `Reset settings` возвращает defaults выбранного модуля, не выключая его. Цвета — `#AARRGGBB`.

## Console Menu: главное меню и загрузка мира

В vanilla title screen нажмите **«Меню NexVisuals»** слева сверху. В оформленном меню есть **«Стиль меню...»** и **Vanilla** для возврата. Либо включите Interface → Console Menu в существующем редакторе. Новый модуль изначально выключен; старые конфиги не включают его автоматически.

- **Classic:** каменные тона, зелёный accent, медленный orbit с лёгким покачиванием.
- **Sunset:** тёплые янтарные цвета и движение камеры из стороны в сторону.
- **Moonlight:** сине-серая палитра и спокойное круговое движение.
- **Still:** Classic без движения фона, появления/hover-анимации кнопок и пульсирующего splash-текста.

Можно независимо задать accent, цвет/alpha панели, цвет текста, tint/alpha панорамы, расположение панели Left/Center/Right. Motion: Still/Orbit/Sway/Drift; доступны speed, amplitude, tilt, starting direction и reverse. Скорость учитывает Minecraft Panorama Scroll Speed; ноль останавливает фон. Button entrance задаётся в миллисекундах, Reduced motion выключает декоративное движение. Drifting pixels — небольшой ограниченный набор фоновых пикселей. `Reset settings` восстанавливает исходные значения.

Сохраняются родные panorama/resource pack, логотип Minecraft/Java Edition, шрифт, localized labels, Singleplayer/Multiplayer/Realms/Options/Quit, language/accessibility, demo restrictions, tooltips и keyboard navigation. Меняется расположение и вид существующих кнопок, а не их действия. Анимация двигает сам widget вместе с его hit area.

**World loading theme** оформляет обычный `LevelLoadingScreen`: панорама, тонировка и декоративные блоки внизу. Реальная карта генерации chunks, progress, narration и момент входа в мир остаются vanilla. Декоративные блоки не изображают процент готовности. Nether/End portal screens и начальный Mojang resource-loading splash не заменяются.

Это собственное оформление в духе консольного Minecraft, без копирования Xbox artwork, звуков или кода. Сторонние title-screen replacements и добавляемые ими кнопки пока не проверены. Настройки остальных экранов/контейнеров остаются в Menu Backdrop/Container Visuals; это не глобальная замена всех GUI.

## Hit Effects и Sounds

| Стиль | Геометрия и поведение |
| --- | --- |
| Burst | Радиальный выброс круглых мягких motes; уменьшение при разлёте |
| Sparks | Узкие яркие искры, более высокая скорость, gravity и короткая жизнь |
| Rings | Два расширяющихся кольца разного радиуса и дополнительные motes |
| Slash | Две пересекающиеся текстурные дуги с разным направлением вращения |
| Impact | Центральная звезда-вспышка, expanding ring и радиальные sparks |
| Classic | Старый emitter foundation; для него дополнительно включается Cosmetic Particles |

Пять новых основных стилей работают при включении одного **Hit Effects**. Настройки: primary/secondary ARGB, общая opacity, размер, intensity/count, lifetime, spread, speed, gravity, fade, luminous appearance, random roll, scale-animation override, easing и accent components. Marker и слабая screen flash включаются отдельно. Glow — яркость и мягкая маска, без bloom pass. Эффекты не видны сквозь стены.

Hit Sounds — отдельный модуль: Soft, Click, Pop, Bell, Metallic, Arcade, volume, pitch, random variation и Preview. Используются ссылки на vanilla sound events, ни одного чужого audio-файла в JAR нет. См. [происхождение assets](../ASSETS.md).

Обе функции реагируют на легитимную локальную попытку атаки по видимой сущности. Сервер может отклонить урон; эффект не означает подтверждённый hit. Попадания стрелами и скрытые/удалённые события не детектируются.

## Viewmodel / Swing / Shield / Fire

- **Viewmodel:** X/Y/Z, pitch/yaw/roll, общий scale и множители scale X/Y/Z для main/offhand. Independent offhand, Copy, Mirror, reset. Presets: Vanilla, Compact, Low, Centered, PvP, Cinematic. Если Separate off hand выключен, используются настройки main hand.
- **Item Swing:** Vanilla, Smooth, Swipe, Slash, Spin, Push, Custom. Visual duration 100–900 ms, amplitude, easing, attack/return balance. Custom задаёт translation X/Y/Z, pitch/yaw/roll и peak scale; старт и конец совпадают со статичным Viewmodel. Spin совершает полный оборот; amplitude регулирует lift, чтобы не было скачка в конце.
- **Shield:** независимые resting/blocking X/Y/Z, scale, pitch/yaw/roll. Vanilla, Compact, Minimal. Добавляется после Viewmodel и может использоваться без него. Opacity shield не реализована: её стабильное применение ко всем составляющим item submission потребовало бы более широкого вмешательства.
- **Fire Overlay:** видимость вплоть до полного скрытия first-person flames, height/width, X/Y, opacity. Vanilla, Low, Minimal, Hidden. Burning state, damage и world fire неизменны.

Swing заменяет обычный WHACK-path first-person item animation. Пустая рука, maps, bow charging, eating/drinking, auto-spin и spear STAB сохраняют специальные vanilla animations. Полный curve editor, arbitrary start/end keyframes и изменение equip animation отложены. Сеть, cooldown, hitbox и reach не меняются.

## Trails и Cosmetics

**Player Trails:** Motes, Silk, Sparks, Rings, Rainbow. Готовые варианты Silk, Ember, Prism, Ripples. Primary/secondary ARGB, life, size, density, minimum movement, emitter height, fade и third-person-only toggle. Silk — перекрывающиеся soft billboards, а не непрерывная геометрическая лента. Trail принадлежит только локальному игроку; исчезает по bounded lifetime.

**Cosmetic Hat:** conical/China Hat с radius, height, vertical offset, ARGB tip/rim, gradient, brim outline и rotation. Presets Prism, Straw, Midnight. Только видимый локальный игрок, third person, обычный entity feature layer и depth test. Другие игроки не получают косметику по сети. В first person шляпа не рисуется.

**Fireflies:** мерцающие огоньки со спокойным дрейфом вокруг локального игрока. Population 4–80, radius 3–14 blocks, size, speed, drift distance, два ARGB-цвета, lifetime 30–160 ticks. Presets Meadow / Embers / Moonlit. Meadow и Moonlit по умолчанию появляются под открытым небом вечером/ночью; Embers разрешает любое время суток. Это светящиеся частицы с обычной глубиной, без освещения блоков. Не более 3 попыток появления за tick и 96 живых частиц; в твёрдых блоках/не загруженных chunks не создаются.

**Elytra Trails:** два симметричных потока у локального летящего игрока. Aurora — мягкие cyan/violet потоки; Comet — короткие тёплые искры; Halo — пары расширяющихся колец. Настройки spacing, width, density, lifetime, два ARGB-цвета и реакция ширины на скорость. По умолчанию видны только в третьем лице. До 16 particles/tick; следы сбрасываются при смене мира/прекращении полёта, большие скачки позиции не соединяются полосой. Положение emitter приблизительное, следует yaw игрока, а не точной анимации elytra bones. Это частицы, не геометрические ленты.

## Custom и vanilla HUD

Foundation сохранён: Custom Crosshair, Coordinates, FPS, Facing, Speed, Player Status, Movement Keys, Camera, Menu Backdrop, Screen Tint и HUD Editor.

Crosshair поддерживает формы Cross, Dot, Circle, Chevron; width/height, thickness, gap, center dot, outline thickness/color и ARGB. Встроены стили Classic, Dot, Wide, Precision, Orbit, Chevron; vanilla attack indicator сохраняется. Circle/Chevron используют кэшированные pixel spans, обновляемые только при изменении геометрии. Динамического target-aware прицела нет.

Через общий **Vanilla HUD** framework доступны hotbar, health, armor, hunger, experience/info bar, experience level, status effects и boss bars. Для каждой группы: offsets X/Y, scale, normalized editor position, visibility, backplate ARGB, outline ARGB, presets Vanilla/Compact/Framed. Hotbar дополнительно умеет рисовать плавную accent рамку выбранного слота.

**Mini HUD (0.2.1):** исправлено уменьшение вокруг левого края каждого индикатора, из-за которого HUD разъезжался. Нижние слои теперь масштабируются вокруг общего bottom-center. У **Hotbar / Mini HUD** появился preset **Mini** и настройка **Linked Mini HUD**, включённая по умолчанию. Когда этот модуль активен, связанные health/armor/hunger/experience/level, air/mount health и item name получают точно такое же преобразование, даже если их отдельная кастомизация выключена. Offhand и attack indicator входят в исходный слой хотбара.

В HUD Editor связанная группа имеет один drag target. Отдельные positions/scales не стираются: Link OFF возвращает независимый режим. Настройки видимости/цвета/фона дочернего модуля продолжают действовать, если он включён. Boss bars и status effects не входят в нижнюю группу. Рамка группы приблизительная; очень длинные названия и большое число дополнительных сердец могут выходить за её editor outline, но не обрезаются им.

**Keystrokes:** вместо строки дефисов по умолчанию рисуются W/A/S/D и Space как плитки с подсветкой нажатия. Подписи берутся из реальных key mappings (длинные сокращаются по ширине). Есть цвет нажатия; `Key tiles OFF` возвращает прежний текстовый режим. Scale, позиция, фон и HUD Editor используют существующие настройки.

HUD Editor: drag, Shift для snap к 8 GUI pixels, arrows для точного перемещения, right-click для enabled, `H` для скрытия toolbar, Settings для выбранного элемента. Выключение Vanilla HUD module восстанавливает обычную отрисовку; `Visible` внутри включённого модуля скрывает слой. Рамки vanilla элементов примерные, стандартные sprites и внутренняя компоновка сохраняются. Общий alpha/tint всех health sprites и spacing сердечек не реализованы.

## Containers и vanilla interfaces

`Container Visuals` использует общую базу `AbstractContainerScreen`: Inventory, Chest, Crafting, Furnace/Blast Furnace/Smoker, Anvil, Enchanting, Brewing, Shulker, Hopper и остальные стандартные экраны, которые вызывают эти методы. Это покрытие по структуре Minecraft; визуальный runtime каждого экрана ещё не проверен.

Реализованы panel tint, border, rounded decorative panel, slot tint/outlines, smooth hover remnants, click ripple/outline и observed quick-move ghosts. Встроенные Clean/Aurora/Snap. Настройки duration, easing, intensity, ghost scale и дополнительный afterimage outline.

Transfer styles: Slide, Smooth, Arc, Pop, Fade. Полный ghost path рисуется только при однозначном локальном изменении количества того же item+components; Fade подсвечивает destination. Обычные pickup/right-click/drop/drag дают безопасный slot pulse. При неизвестном destination не рисуется выдуманная траектория. Реальные items и packets остаются под контролем Minecraft.

Opening Fade/Scale/Slide анимирует декоративную рамку; slots и их hit areas остаются на обычных местах. Полная замена текстур панелей, alpha предметов, closing animation и полностью движущийся container screen отложены. Прозрачность фона вокруг контейнера настраивается отдельным **Menu Backdrop**.

## Sky и shaders

**Sky Palette**: Vanilla, Night blue, Sunset, Dark, Cosmic + custom sky/horizon color and blend. Меняются два цветовых поля vanilla Overworld sky render state. Никакого изменения времени суток, moon phase, terrain light или fog. Nether/End остаются обычными. Это палитры существующего sky, не полноценные texture/cubemap skyboxes.

Если установлен Iris, **Sky Palette автоматически не применяется**. В General появляется **Open Iris shader settings**, использующий только публичный `IrisApi.openMainIrisScreenObj`. Он не скачивает packs, не выбирает их и не редактирует private shader options.

Для изученной ветки [Iris 1.21.11 public API](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java) и [IrisApiConfig](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/api/java/net/irisshaders/iris/api/v0/IrisApiConfig.java) нет общего публичного API применения именованных наборов настроек произвольных shader packs. Поэтому **shader presets и собственный post-processing engine не реализованы**. Наличие кнопки интеграции не является подтверждением runtime-совместимости.

## Profiles и UI

Module preset меняет настройки одного модуля. Local Profile сохраняет весь NexVisuals. Можно сохранить несколько профилей, применить, обновить, переименовать, удалить с подтверждением и восстановить defaults. Названия могут содержать пробелы и кириллицу.

В Profiles доступны три готовых **Global Styles**. `Apply style` заменяет текущую конфигурацию выбранным набором; сначала сохраните свой профиль, если хотите к нему вернуться. Пользовательские profile-файлы не изменяются.

- **Clean:** компактный Viewmodel/Shield, cyan dot, FPS, плавный selected-slot accent и быстрый неброский container feedback.
- **Aurora:** violet circle, cyan/violet Impact, Bell sound, Slash swing, Silk trail, Prism hat и согласованное оформление контейнеров.
- **Cinematic:** более крупный повёрнутый Viewmodel, медленный Smooth swing, уменьшенное bobbing, тёплые Rings/Trails, Sunset palette и плавный интерфейс.

Рецепты лежат в `src/main/resources/nexvisuals/presets/`. Это обычный JSON той же схемы, загружаемый только по явному действию; они не содержат исполняемого кода.

Из референсов взяты общие идеи: прозрачность панелей, цветовой accent, быстрые swatches, встроенные стили, плавная рамка selected hotbar и реакции слотов. Artwork, layout, логотипы и код других клиентов не копировались. В меню нет account management, ESP/chams, автоматизации инвентаря или remote sharing.
