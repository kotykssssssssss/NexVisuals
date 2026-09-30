# Возможности 0.5.0-dev

Все модули выключены по умолчанию. В мире откройте меню **Right Shift** (переназначение через Minecraft Controls) или кнопкой NexVisuals в pause menu. Выберите модуль, включите его, выберите встроенный стиль кнопкой `>` и нажмите `Apply`. `Reset settings` возвращает defaults выбранного модуля, не выключая его. Цвета — `#AARRGGBB`.

## Дополнения 0.5

**Particles:** Jump / Landing Rings (Double Halo / Runic / Quiet), Footstep Effects (Light Prints / Petal Walk / Ember Steps / Ripples), Totem Echo (Golden Helix / Phoenix / Supernova), Block Interaction FX (Crystal / Workshop / Garden). **World:** Cosmetic Orbitals (Atom / Starlight Crown / Spiral). **Post Processing:** Weather Lens (Drizzle / Storm Glass / Mist), Underwater FX (Quiet Water / Lagoon / Deep), Retro Display (Console CRT / Pixel Adventure / Clean CRT / Soft Mosaic).

Это восемь самостоятельных модулей, каждый с настройками, reset и сохранением через прежние config/profiles. Новые world-image модули можно включать без Lightweight Shaders; все четыре используют один общий scene copy/pass. Условия активации, лимиты и ручные проверки описаны в [new-visuals](new-visuals.md). Старые модули и ночной Skybox не переписаны.

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

## Live Background

**Interface → Live Background** — процедурные GPU-обои, независимые от расположения и действий кнопок Console Menu. Работают на vanilla/Console title screen, в NexVisuals editor и Profiles; pause menu включается отдельно. Loading screens, containers и игровой HUD не заменяются. Выберите `Background: Vanilla` или выключите модуль для прежнего фона без потери цветов. Кнопка Vanilla в Console Menu меняет его оформление отдельно от обоев.

| Preset | Форма и движение |
| --- | --- |
| NexVisuals | Несколько диагональных violet/cyan лент с мягкой глубиной и тонкими светлыми краями |
| Aurora | Горизонтальные изогнутые световые занавеси с медленным движением полос |
| Flow | Плавно дрейфующие эллиптические цветовые поля без лент и частиц |
| Nebula | Два масштаба процедурного облачного поля, тёмные violet глубины, редкие motes |
| Waves | Широкие S-образные зелёные полосы, теневой край и светлая кайма по направлению dashboard reference |
| Minimal | Спокойный диагональный gradient с едва заметным движением |

Три ARGB-цвета задают primary/secondary/accent layers; alpha регулирует вклад каждого. Speed и Motion amount управляют движением, ноль замораживает фон; Console Menu Reduced motion также останавливает его. Intensity, Brightness, Saturation, Shape softness, Subtle motes (0–32) и Background dim изменяют вид сразу за редактором. Softness — аналитическая мягкость форм, не дорогой blur. Motes — ограниченная GPU-сетка, приблизительное количество, без particle objects и истории.

В title screen фон занимает один fullscreen triangle с 96-byte uniform block, без дополнительных framebuffers. В editor/Profiles/pause он рисуется в переиспользуемый canvas половинной ширины/высоты окна и передаётся штатной GUI-очереди одним textured quad. Это сохраняет порядок после игрового HUD и перед menu controls; canvas пересоздаётся только при resize и освобождается при возвращении на title/выходе/выключении. Scene copy, readback, CPU meshes и внешние assets не нужны. Время монотонное, независимо от FPS. Выход из поддерживаемого меню останавливает clock без catch-up. При shader/draw failure возвращается обычный фон; статус виден в редакторе, OFF/ON повторяет попытку. При активных обоях старые Console tint/pixels не накладываются второй раз, панель/кнопки остаются. Preview/F4 в мире скрывает обои вместе с редактором, чтобы оценить sky/post effects.

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

## Custom Skybox

**World → Custom Skybox** — отдельный модуль, рисующий собственный сферический купол и при выборе Custom Stars отдельную геометрию звёзд. Это не просто изменение двух vanilla цветов. Купол содержит плавный трёхцветный gradient, процедурную туманность, ленты северного сияния, редкие метеоры и мягкий glow горизонта. Aurora, Nebula, Shooting stars и Horizon glow включаются независимо. Собственные программы GLSL работают через Minecraft `RenderPipeline`, без прямого OpenGL или стороннего shader pack.

General: gradient/intensity, horizon height/softness, sky brightness/saturation/RGB tint и day/night influence. Последняя настройка смешивает реальные day/sunset/night палитры с day palette, не меняет часы мира. Для Day, Sunset/Sunrise и Night отдельно задаются sky/horizon/zenith RGB. Переход основан на реальном угле солнца с smoothstep. Alpha sky-палитр не используется; alpha атмосферных/небесных tint умножает их интенсивность. Minecraft сохраняет обычные clouds, rain, sunrise disc, солнце/луну и moon phases.

**Dynamic cycle** добавляет отдельные morning sky/horizon/zenith и day/twilight/night brightness multipliers. Направление реального sun angle различает рассвет и закат; четыре состояния смешиваются непрерывными нормализованными весами, без переключений времени мира. Day atmospheric haze мягко осветляет горизонт; Soft sun halo следует реальному солнцу и его tint/opacity. `Fog follows sky cycle` использует текущую horizon palette, умноженную на Fog color, с обычным Fog blend. Все эти поля редактируемые. В старых presets/config dynamic cycle, haze, halo и cycle fog выключены, чтобы сохранить удачный ночной вид.

Stars: Vanilla / Custom / Disabled. Custom поддерживает 0–4000 звёзд, размер, Pixel/Diamond/Soft, RGB/alpha, brightness и отдельный twinkle каждой звезды с speed/intensity. В Vanilla размер/количество/форма остаются Minecraft-owned; доступна общая пульсация, tint, brightness/opacity. Звёзды появляются по обычной ночной видимости и затухают от дождя. Sun & Moon имеют отдельные size, ARGB tint и opacity, сохраняя реальные пути движения. Fog — RGB/alpha blend и density 1–3: значения больше 1 только усиливают обычный атмосферный fog, никогда не увеличивают visibility distance.

| Preset | Вид и состав |
| --- | --- |
| Vanilla+ | Более глубокий знакомый gradient, vanilla sun/moon/stars и мягкий горизонт |
| Deep Night | Тёмно-синий купол, 2600 twinkling stars и сдержанная зелёная aurora |
| Purple Nebula | Фиолетовая процедурная туманность, крупные мягкие звёзды и лиловый горизонт |
| Sunset | Тёплый широкий gradient, увеличенное золотое солнце и небольшой тёплый fog blend |
| Blood Moon | Большая красная луна, crimson nebula и звёзды-ромбы |
| Cyber | Cyan aurora, magenta nebula, cyan diamonds и редкие meteors |
| Minimal | 600 pixel stars, сниженная saturation, без twinkle/glow/atmosphere motion |
| Enhanced Day | Чистое голубое небо, светлый горизонт, peach dawn, тёплый закат; лёгкие haze/halo и familiar blue night |
| Dynamic / NexVisuals | Blue day → мягкий morning → rose-orange dusk → violet night, restrained nebula/aurora, звёзды и meteors; плавные brightness/fog transitions |

Настройки применяются в памяти сразу. Preset — исходный набор параметров; редактирование не вызывает его повторного применения. `Current: Custom` появляется при отличии от всех готовых наборов. Разделы General / Day / Sunset / Night / Dynamic cycle / Stars / Sun & Moon / Atmosphere / Fog доступны в существующем редакторе. Reset сбрасывает **весь** модуль даже при выбранном разделе. Preview/F4 скрывает редактор без закрытия и записи промежуточной конфигурации; F4/Esc возвращает настройки.

Skybox работает в Overworld air; Nether/End, water/lava/powder snow, Blindness/Darkness остаются обычными. Это процедурный skybox, без загрузки пользовательских cubemap/texture files. Старый **Sky Palette** сохранён для старых конфигов; активный новый Skybox имеет приоритет. Sky Palette по-прежнему отключает собственный tint при любом установленном Iris.

## Lightweight Shaders / Post Processing

**Post Processing → Lightweight Shaders** — эффекты изображения мира после vanilla entity post effects и перед HUD/UI. Общая intensity; color grading можно выключать независимо. Параметры: brightness, contrast, saturation, bounded gamma, temperature, green/magenta balance, **Exposure** (−0.65…0.65 stops) и **Highlights enhancement**. Последнее работает только в ярком диапазоне. Это display color curves, без изменения block light. Чёрный остаётся чёрным в color grading.

Независимые эффекты: vignette с intensity/radius/softness/ARGB, selective glow, chromatic separation до 3 pixels, multiplicative film grain с size/intensity, color filter, local night tint и damage flash по обычному `hurtTime`. **Peripheral softness** мягко сглаживает только периферию четырьмя соседними samples, оставляя центр и последующий HUD/UI резкими. Grain обновляется на 24 Hz, ослаблен в тенях/ярких участках и сохраняет black.

**Compact glow** остаётся четырьмя bright-neighbor samples. **Wide glow** извлекает яркие участки по luminance с мягким threshold в переиспользуемый target размером ¼ ширины и ¼ высоты окна, затем реконструирует мягкий ореол девятью взвешенными samples. Радиус 1–12 display pixels. Это локальное LDR glow, **не HDR bloom**: яркость ниже threshold не поднимается всем экраном. Wide требует один дополнительный маленький pass; Compact — только основной pass. Нет depth effects, теней, SSR, DOF, volumetrics или управления чужими shader packs. Дополнительная flash не заменяет vanilla damage information.

| Preset | Настройки |
| --- | --- |
| Vanilla+ | Обычные цвета и слабая vignette |
| Vibrant | Более насыщенные цвета, contrast, selective highlights и Compact glow |
| Cinematic | Приглушённые тёплые цвета, зерно, vignette, Wide glow и мягкая периферия |
| Cold | Холодный баланс, умеренная saturation и blue night tint |
| Warm | Золотистый баланс, слабый glow и чуть больше saturation |
| Night | Более тёмные холодные цвета, усиленный night tint |
| Retro | Сниженная saturation, более крупное grain и небольшое RGB separation |
| Dreamy | Широкие мягкие ореолы ярких участков и заметнее сглаженная периферия при почти естественной палитре |

Все новые модули изначально выключены и используют обычные config/profiles schema 1. Отсутствующие новые поля получают defaults; старые ID и global profiles не меняются. Старые post configs используют Compact, нулевую Exposure и выключенную Peripheral softness. Общие Clean/Aurora/Cinematic profiles сбрасывают неуказанные новые модули; сначала сохраните собственный профиль.

### Iris и fallback

Если публичный [`IrisApi.isShaderPackInUse()`](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java) сообщает активный shader pack, Skybox и Lightweight Shaders **приостанавливают GPU hooks**, сохраняя enabled/settings. Причина видна в GUI. Если API не удаётся безопасно проверить, применяется тот же fallback. При установленном Iris без активного pack новые модули могут работать; это требует runtime проверки. Iris не обязателен. General → Open Iris shader settings использует существующий public API, не выбирает и не скачивает packs.

Live Background рисуется отдельно в UI и не входит в world shader chain; установленный Iris не блокирует его намеренно. Практическая совместимость новых sky/post/wallpaper paths с конкретными Sodium/Iris JAR пока не подтверждена — пользователь проверяет её вручную. Ни один из этих модов не является обязательной зависимостью NexVisuals.

GPU compile/draw failure отключает соответствующий optional pass до OFF/ON и записывает ошибку один раз в `latest.log`. Pipelines компилируются по требованию, не включены в список обязательных vanilla pipelines. Post-processing также приостанавливается при Blindness/Darkness у camera entity. Наличие guard **не подтверждает** практическую совместимость с Sodium/Iris: она оставлена пользователю для проверки.

## Profiles и UI

Module preset меняет настройки одного модуля. Local Profile сохраняет весь NexVisuals. Можно сохранить несколько профилей, применить, обновить, переименовать, удалить с подтверждением и восстановить defaults. Названия могут содержать пробелы и кириллицу.

В Profiles доступны три готовых **Global Styles**. `Apply style` заменяет текущую конфигурацию выбранным набором; сначала сохраните свой профиль, если хотите к нему вернуться. Пользовательские profile-файлы не изменяются.

- **Clean:** компактный Viewmodel/Shield, cyan dot, FPS, плавный selected-slot accent и быстрый неброский container feedback.
- **Aurora:** violet circle, cyan/violet Impact, Bell sound, Slash swing, Silk trail, Prism hat и согласованное оформление контейнеров.
- **Cinematic:** более крупный повёрнутый Viewmodel, медленный Smooth swing, уменьшенное bobbing, тёплые Rings/Trails, Sunset palette и плавный интерфейс.

Рецепты лежат в `src/main/resources/nexvisuals/presets/`. Это обычный JSON той же схемы, загружаемый только по явному действию; они не содержат исполняемого кода.

Из референсов взяты общие идеи: прозрачность панелей, цветовой accent, быстрые swatches, встроенные стили, плавная рамка selected hotbar и реакции слотов. Artwork, layout, логотипы и код других клиентов не копировались. В меню нет account management, ESP/chams, автоматизации инвентаря или remote sharing.
