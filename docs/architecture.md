# Архитектура NexVisuals

Проект остаётся клиентским Fabric-модом для **Minecraft 1.21.11**. `src/main` — обычная Java-логика и ресурсы, `src/client` — интеграция с игрой. Серверного entrypoint нет.

## Регистрация и жизненный цикл

`ClientModules` создаёт реальный каталог модулей и общие сервисы. Он не открывает окна и не регистрирует hooks. `NexVisualsClient` связывает каталог с Fabric, загружает конфигурацию, регистрирует keybind и сохранение. Узкие Mixins получают те же экземпляры через entrypoint.

`VisualModule` содержит ID, название, описание, категорию, настройки, пресеты и необязательные команды редактора. Включение проходит через `onEnable`, выключение — через `onDisable`. Все модули по умолчанию выключены. `ModuleRegistry` отвергает повторяющиеся ID.

Модуль не должен загружать файлы или регистрировать новый event listener при каждом включении. Hooks устанавливаются один раз и проверяют текущие значения в памяти.

## Settings и presets

`Setting<T>` хранит типизированные default/current значения, ID, label и описание. Реализации поддерживают boolean, int, double, диапазон, enum, ARGB, текст и keybind. Числа clamp-ятся; NaN/Infinity и ошибочные типы отвергаются.

`ModulePreset` — набор обычных JSON-значений настроек. Применение явное: сначала defaults текущего модуля, затем значения пресета. Состояние enabled не меняется. Если в пресете ошибка, все прежние настройки восстанавливаются. Это позволяет применить стиль, а затем свободно менять sliders; загрузка config не запускает preset заново.

`VisualModule.group(name, settings...)` добавляет необязательные разделы большого редактора. `Setting.revision` изменяется только при изменении корректного значения. `currentPresetName()` сравнивает настройки с полными recipe/defaults и кэширует результат до следующей revision, поэтому профиль или ручное редактирование не оставляет ложный preset label. `runtimeStatus()` — необязательное сообщение о текущем состоянии renderer. GUI строит section selector и live status без branches для отдельных модулей; tooltip обновляется при изменении статуса. Preview/F4 скрывает только editor drawing и блокирует ввод в скрытые controls; экран не закрывается и не передаёт clicks игре.

`ModuleAction` — внутренняя Java-команда редактора, например Copy/Mirror hand или Preview sound. Она не сериализуется и не может поступить из файла профиля.

`NexVisualsScreen.rebuildWidgets` сохраняет focus/cursor поискового поля и восстанавливает их **после** native rebuild. Minecraft 1.21.11 вызывает `setInitialFocus()` после `init()`: восстановление только внутри `init()` перехватывалось этим последним проходом, и ввод прекращался после фильтрации. Поле получает focus обратно только если оно имело его до rebuild, поэтому клики по другим controls не переводят ввод в поиск.

`BuiltinProfile` перечисляет три bundled global recipes, которые `ProfileManager.applyBuiltin` загружает через тот же ConfigManager. Путь задаётся только enum, а не текстом пользователя. Catalog tests проверяют, что все module/setting IDs рецептов существуют, и что итоговые конфигурации различны и сохраняются.

`SettingControls` — единая фабрика controls; `NexVisualsScreen` автоматически выводит настройки, selector пресетов и actions. Новому модулю не нужен собственный settings screen. Для цветов есть ARGB-поле, шесть быстрых swatches и общий `ColorPickerScreen`. `core.color.HsvColor`/`ColorPickerModel` выполняют HSV/RGB conversion, сохраняют alpha и выбранный hue на сером/чёрном. Палитра использует стандартные GUI quads и widgets; отдельные textures/framebuffers ей не нужны. Цвет действует в памяти сразу; Cancel восстанавливает значение до открытия. Live Background поддерживает этот экран, сохраняя прежний shader/canvas.

## Config и профили

Основной файл: `<Fabric config directory>/nexvisuals.json`. В development client это `run/config/nexvisuals.json`.

Профили: `<Fabric config directory>/nexvisuals/profiles/<name>.json`. Схема одна:

```json
{
  "schemaVersion": 1,
  "global": { "accent_color": "#FF8B9DFF", "panel_opacity": 0.9 },
  "modules": {
    "hit_visuals": {
      "enabled": true,
      "settings": { "preset": "IMPACT", "size": 1.0, "primary": "#EE78D9FF" }
    }
  },
  "gui": { "category": "COMBAT", "module": "hit_visuals" }
}
```

Это сокращённый пример: при сохранении записываются все известные настройки. Отсутствующие поля получают defaults; неизвестные поля игнорируются и при следующей записи не сохраняются. Ошибки отдельных значений дают warning и default. Повреждённый JSON/UTF-8 копируется в `.broken-<timestamp>.json`; ошибка чтения/backup блокирует автоматическую запись текущей сессии. Запись идёт через соседний временный файл с atomic move, если файловая система его поддерживает.

Имена профилей: до 48 букв/цифр, пробелы, `_`, `-`; без путей, точек, ведущих/замыкающих пробелов и Windows device names. Поддерживаются `My PvP` и кириллица. Rename не перезаписывает другой профиль; Delete подтверждается в GUI. Ошибка чтения повреждённого профиля сохраняет активные настройки. Файлы не выполняют код. Основные ID foundation сохранены, включая `viewmodel`, `hit_visuals`, `hud_coordinates` и `custom_crosshair`.

## Rendering

- `HudDispatcher` связывает capability `HudModule` с Fabric `HudElementRegistry`, сохраняя vanilla render conditions.
- `Draw` предоставляет маленький слой GUI-примитивов и clipping поверх `GuiGraphics`. Прямого OpenGL или собственного framebuffer engine нет; optional post pass использует стандартный Minecraft `TextureTarget`.
- `ReticleMask` строит disjoint fill/outline pixel spans для Circle/Chevron. `CustomCrosshairModule` кэширует маску по geometry key; цвет не требует перестроения, ring rasterization не выполняется каждый кадр.
- `TextHudModule` объединяет координаты, FPS, направление, скорость, локальный статус и movement keys.
- `VanillaHudModule` оборачивает 8 стандартных Fabric HUD layers. Позиция, scale, видимость, backplate и outline используют одну реализацию. Внутреннее содержимое рисует Minecraft; здоровье/голод/эффекты не вычисляются заново.
- `HudTransform` задаёт одну affine-матрицу для нижних слоёв относительно bottom-center. Hotbar может владеть связанной Mini HUD-группой; дочерние модули наследуют именно её transform, не умножая scale повторно. `HudModule.isHudActive` позволяет применить группу к слоям с выключенной отдельной кастомизацией. Air/mount/item-name layers подключаются к той же группе через HUD API; новых Mixins нет. В editor отображается один групповой drag target.
- `EditableHud` + `HudPosition` связывают renderer и drag editor. После перетаскивания сохраняется нормализованная позиция с ограничением viewport. Для vanilla layers рамки приблизительные: дополнительные сердца, длинные boss bars и offhand могут выходить за них.
- `EffectEmitter` отправляет `EffectParticle` в стандартный translucent particle pipeline. Исходные маски — оригинальные PNG; atlas JSON добавляет sprites через resource stack, не заменяет vanilla particle textures. Цвет, размер, roll, gravity, lifetime и fade принадлежат самой частице.
- `PlayerTrailsModule` хранит только предыдущую позицию; остальную историю представляют ограниченные временем частицы Minecraft.
- `ElytraTrailsModule` хранит две последние точки; `FlightTrail` вычисляет симметрию/лимит samples. `FirefliesModule` использует `ParticleCadence` и стандартные `FireflyParticle` с аналитическим drift/twinkle, отдельным cap 96 и общим emitter budget. Оба модуля используют уже имеющийся particle atlas.
- `HatFeatureLayer` регистрируется через Fabric living feature callback. Геометрия следует head transform и проходит обычную глубину/видимость player renderer. Отрисовка только локального видимого игрока в third person.
- `LocalAttackFeedback` принимает только локальную атаку по текущему видимому entity hit result. Возвращает `PASS`. Эффект/звук означает попытку атаки, **не подтверждение урона сервером**.

## Skybox и world-image effects

`SkyboxModule` объявляет typed settings / groups / presets, получает immutable `Frame` при vanilla sky extraction. `SkyMath.weights` сохраняет прежние три палитры; opt-in `SkyMath.cycle` разделяет twilight на morning/sunset по направлению реального sun angle. Четыре веса нормализованы, непрерывны и периодичны; brightness также интерполируется. Старые presets/config не включают этот режим автоматически. Direction/phase celestial objects не подменяются. `SkyboxRenderer` заменяет только sky disc sphere mesh (32×16 quads), а Custom Stars — bounded field. `StarField` создаёт детерминированное распределение unit-sphere; GPU geometry меняется только при правке количества/размера. UBO передаёт цвета/weights/time, дневную haze и sun halo с реальным направлением солнца. Nebula, aurora и один meteor на интервал — аналитические fragment layers, без сущностей, particles/history или скрытой информации. Vanilla clouds/precipitation/sunrise disc сохраняются.

`SkyRendererMixin` — узкие adapters existing sky methods, scale constants X/Z и color modulator sun/moon/stars. У size hooks `require=2` соответствует двум scalar constants каждой vanilla scale. `FogRendererMixin` меняет computed atmospheric color перед UBO upload, чтобы clear color совпадал с fog. Все шесть дистанций делятся на density 1–3, без расширения видимости. Skybox применим только к Overworld air и уступает Minecraft vision/fluid rules; старый Sky Palette остаётся fallback при неактивном новом модуле.

`PostProcessingModule` владеет settings/presets и guard, `PostProcessingRenderer` — переиспользуемым color-only `TextureTarget`. После vanilla world/entity post chains, перед HUD/UI: main color копируется в target, fullscreen triangle рисует grading и эффекты обратно. Compact glow выполняется там же. При Wide `glow_extract` сначала выделяет luminance highlights в target размером ceil(width/4) × ceil(height/4), основной pass реконструирует glow девятью weighted linear samples. Small target/ring освобождаются при Compact/OFF; resize меняет только размеры targets. Peripheral softness использует четыре соседних scene samples и плавный radial mask, HUD/UI не затрагивается. Grain — bounded arithmetic hash с 24 Hz time cells. Depth не копируется/не меняется, readback отсутствует. `ColorGrade` — CPU reference с exposure/highlights, его tests не подтверждают изображение GPU. Draw/shader failure закрывает optional resources, пишет одну error и требует OFF/ON для retry.

Pipelines строятся через `RenderPipeline` snippets и не регистрируются как обязательные vanilla shaders: draw проверяет cached `GpuDevice.precompilePipeline().isValid()`. Это позволяет fallback при недоступном custom shader, без принудительного падения всего vanilla resource reload. Native uniform rings: SkyConfig 176 bytes, StarConfig 48, VisualConfig **272** (17 vec4), HighlightConfig 16, BackgroundConfig 96. GLSL validator отражает и проверяет эти layouts отдельно от Java build. Выключение и shutdown освобождают buffers/targets; cached meshes не перестраиваются каждый кадр. При resource reload используется shader cache Minecraft; успешность reload/GPU output проверяется вручную.

### Дополнительные cosmetics и общий world-image pass

`JumpRingsModule` наблюдает реальные local ground/upward/descent transitions через `GroundMotion`; `FootstepEffectsModule` использует distance-based `StepCadence`. `EffectEmitter.ground` добавляет обычный particle quad с постоянной горизонтальной ориентацией. Camera-facing `EffectParticle` для прежних модулей не меняется. Все новые particles разделяют существующий per-tick budget и cap, исчезают при выключении владельца и не хранят world history. `OrbitalsModule` создаёт до 12 краткоживущих точек через два тика, только возле видимого локального игрока в third person.

`ClientPacketListenerMixin` вызывает Totem Echo / Totem Pop Sounds / Totem Tracker после настоящего vanilla event 35, только когда его entity — local player. Vanilla particles/item animation не отменяются; отдельный `ModifyArgs` меняет только native local totem volume/pitch по настройкам звукового модуля. `BlockPlacementMixin` наблюдает `useItemOn` HEAD/RETURN без изменения return value: module использует vanilla `BlockPlaceContext`, сохраняет одну candidate cell и expected BlockItem block, затем проверяет реальный локальный state change. Break callback — Fabric API. Перенаправляющие placement contexts и другие block variants сознательно могут пропускаться; packets, prediction и серверная логика не меняются.

`WeatherLensModule`, `UnderwaterEffectsModule`, `RetroDisplayModule` содержат только независимые metadata/settings. `WorldImageExtras` объединяет их для existing `PostProcessingModule` и renderer: один scene copy и fullscreen triangle, 6 дополнительных vec4. Старый grading mix применяется отдельно, поэтому его disabled/intensity=0 не выключает новые модули. `EnvironmentEnvelope` рассчитывает переходы по game time с одинаковым результатом при разном FPS; пауза замораживает эффекты, смена мира сбрасывает wetness. Weather/biome/fluid checks выполняются не чаще одного раза за тик и отсутствуют при одном Retro Display. GPU pass не запускается в сухом воздухе, если grading/Retro не включены; при всех отключённых модулях ранний выход до camera/compatibility probes. Ошибка GPU pass логируется один раз, блокирует повтор до переключения модуля. При active Iris pack или Blindness/Darkness общая image pass приостанавливается. Runtime status остаётся частью generic GUI.

`RenderCompatibility` обнаруживает Iris через Fabric Loader и один раз связывает **публичный** `IrisApi.isShaderPackInUse` с MethodHandle. В кадре нет reflection search. Активный shader pack или недоступный API блокирует оба новых rendering paths с сообщением в generic GUI; settings остаются сохранёнными. Post target освобождается при блокировке. При установленном Iris без pack новые paths допускаются, но практическая совместимость не проверена. Нет управления private shader options или скачивания packs.

## Дополнение HUD 0.6

`EquipmentHudModule`, `ItemCounterModule`, `StatusEffectsHudModule` и `ActiveModulesHudModule` расширяют существующий `TextHudModule`: общий background, color, scale, anchor/relative placement и `EditableHud`. `HudDispatcher` регистрирует их обычным способом через Fabric HUD API, новых hooks/Mixins нет. Native item models и effect sprites берутся из Minecraft/resource packs. Каталог создаёт только metadata/фиксированные буферы; `ItemStack.EMPTY`/`Items` впервые используются на игровом tick после bootstrap реестров.

Equipment обновляется на tick, item counter/active list — раз в 4 ticks, status list — раз в 5 ticks. Иконки/прочные labels/таймеры обновляются в этих snapshots; рендер не сортирует список и не сканирует inventory. Буферы оборудования ограничены шестью элементами, HUD lists имеют max rows. Выключенный HUD может один раз заполнить preview в editor, без вычисления каждой строки каждый кадр.

## Звуковой feedback и Totem Tracker 0.7

`HitSoundsModule` использует прежний `LocalAttackFeedback`. `LocalSoundWindow` хранит одну recent action на 450 ms в радиусе 1 block; tick очищает истёкший/world-changed context. `SoundEngineMixin` меняет значение `SoundInstance.getVolume` в native play/calculateVolume, сохраняя instance identity, categories и subtitles. Whitelist — шесть vanilla attack events, только positional PLAYERS sounds. В sound event нет attacker ID: одновременно возникший чужой attack cue в этом малом окне может также приглушиться. Hurt/death не входят в whitelist. OFF/нулевой custom volume/множитель 1 сохраняет native gain; глобальные Minecraft options не изменяются.

`TotemSoundsModule` использует настоящий local event 35: Native Vanilla voice меняет исходный gain/pitch без второй копии; custom voice оставляет регулируемый native underlay и добавляет один локальный cue. Preview не создаёт game events. Каждый модуль удерживает максимум один свой user cue и останавливает только его при следующем cue/OFF; у Hit Sounds сохранено прежнее поведение коротких vanilla voices. Audio resources загружаются штатным SoundManager, не с диска на tick/render. Ограничения повторов — 60 ms hit / 100 ms totem.

`TotemTrackerModule` наследует `TextHudModule`, включая `EditableHud`; считает только event 35 при enabled и обновляет текст раз в секунду/сразу после pop. `ActivationCounter` хранит два числа и weak world token; история сущностей/событий не накапливается. Счётчик session-only, тогда как HUD placement/options включены в общий config/profiles. По умолчанию смена мира/измерения сбрасывает count; ручной Reset counter не меняет настройки.

Шесть новых audio IDs объявлены через resource template `sounds.json`. `processResources` подставляет event fallbacks в обычной сборке либо file references при явном `userSoundResources` внутри проекта. Converter/hash manifest и подготовленные clips — ignored локальные build inputs. Никакой внешний код, runtime download или обязательный decoder не добавлен.

`ReticleMotion` — небольшие чистые envelope-функции от времени/своего movement/swing; pose transform применяется только к custom reticle, vanilla attack indicator рисуется после восстановления pose. Cached `ReticleMask` не перестраивается для каждого animation frame. `HudFeedback` вычисляет low-health severity, два heartbeat peaks, durability и effect durations. Screen Edge Tint получает только здоровье/hurt timer своего игрока. Старые config defaults остаются OFF/STATIC, ID не меняются.

Hearts/Pixels используют существующие `EffectEmitter`/`EffectParticle`, particle atlas и общий budget; это две новые формы в прежнем Hit Effects. Геометрия/движение/параметры различаются, gameplay/пакеты не меняются. Предыдущие Skybox/post shader chains не затронуты.

## Container visuals (прежняя система)

Одна `ContainerVisualState` принадлежит одному `AbstractContainerScreen`. Общие hooks украшают panel и active slots, наблюдают `slotClicked` и рисуют ghosts после обычного содержимого. Они не отменяют clicks, не меняют slots/packets и не задерживают close.

Для `QUICK_MOVE` до/после обычного клиентского действия снимаются counts **одного точного item + components**. `TransferMatcher` возвращает destinations только при полном сохранении количества, отсутствии других уменьшающихся slots и не более 8 destinations. Это наблюдаемое локальное prediction, не подтверждение сервера. При неоднозначном/асинхронном результате — только source pulse. Pickup/right click/drop/quick craft дают slot feedback без выдуманного маршрута.

Opening Scale/Slide/Fade относится к декоративной рамке; интерактивные slots неподвижны. Это сознательное ограничение для правильной геометрии мыши. Иконки ghosts исчезают уменьшением; общий безопасный alpha multiplier для произвольных GUI items не добавлялся.

## Console menu

`ConsoleMenuModule` хранит typed settings и четыре module presets, использует общий config/profile flow. `ScreenEvents.AFTER_INIT` связывает текущий `TitleScreen` с `ConsoleTitleState`. Последний сохраняет original vanilla button instances и их actions/active flags/tooltips/narration, меняя bounds и skin. Временное состояние освобождается при удалении экрана или выключении модуля, восстанавливается через обычный init. Общий settings screen можно открыть из title screen; HUD Editor там неактивен без мира.

`ConsoleLayout` рассчитывает panel/logo/button geometry в GUI pixels; `MenuMotion` — углы только существующего menu cubemap. `MenuClock` использует монотонное время и замораживает движение без catch-up при паузе. После долгого отсутствия меню delta ограничена 250 ms. `ConsoleTheme` рисует beveled pixel-панели и максимум 18 фоновых motes обычными `GuiGraphics` primitives. Новый framebuffer, shader pipeline или копия vanilla assets не создаются. Меню остаётся title screen Minecraft, сетевые действия и ограничения аккаунта не подменяются.

Тема загрузки меняет только background обычного `LevelLoadingScreen.Reason.OTHER`. Progress tracker, chunk map, narration и close/tick принадлежат Minecraft. Начальный resource-loading overlay и порталы не заменяются.

## Live Background

`LiveBackgroundModule` расширяет тот же catalog/settings/groups/presets/config. В нём нет новой GUI-системы. `LiveBackgroundRenderer` использует optional `POST_PROCESSING_SNIPPET` pipeline, 6 vec4 в UBO. Title screen: одна fullscreen triangle прямо в main color target, как native panorama. Editor/Profiles/pause: та же triangle в reused half-width/half-height `TextureTarget`, затем `BlitRenderState` через публичные `GuiGraphics.guiRenderState` / `TextureSetup`, до controls. В Minecraft 1.21.11 HUD извлекается до screen, но рисуется позднее: direct main-target draw в editor оставлял бы HUD поверх обоев. Native queued blit решает порядок без отмены gameplay/HUD hooks; flipped V сохраняет ориентацию framebuffer. Canvas создаётся при первом таком меню/resize, закрывается при title/выходе/OFF. Нет scene copies/readback/CPU meshes. Разные GLSL branches создают ribbons, fields, two-scale noise или gradient; motes — bounded grid hash. Alpha — вклад слоя, фон непрозрачный. Softness математическая, без blur pass.

`PanoramaRendererMixin` в HEAD отменяет весь panorama render вместе с queued overlay только после успешного wallpaper draw. `ConsoleTitleState` пропускает старый tint/pixels, сохраняя panel/widgets. NexVisuals/Profiles вызывают фон до своих overlay/controls; Preview не рисует его. `ScreenMixin` заменяет только опциональный PauseScreen background, сохраняя deferred subtitles; другие screens не затронуты. Настройки Mode: Vanilla и module OFF возвращают existing paths. Console layout и wallpaper включаются независимо.

`MenuClock` обеспечивает монотонное время/FPS independence, speed=0/motion=0/reduced-motion freeze без catch-up. END_CLIENT_TICK закрывает uniform ring вне поддерживаемых menus и при режиме Vanilla; disable/shutdown также закрывают его. GPU failure выдаёт одно сообщение/log и откатывается к ordinary background до OFF/ON. Live не подключается к world shader chain и не блокируется Iris pack guard; это архитектурное разделение, не подтверждение runtime совместимости.

## Mixins: зачем они нужны

| Адаптер | Узкая ответственность |
| --- | --- |
| `ItemInHandRendererMixin` | Viewmodel/Shield в pushed hand matrix, обычный `swingArm`, scoped original held model submit для Weapon Trails |
| `LocalPlayerSwingMixin` | Наблюдает принятый vanilla local swing в RETURN; запускает только cosmetic clocks/history, не меняет player state/packets |
| `ItemModelTrailMixin` | Пассивно пробует первый item layer после ItemTransform.apply в scoped local weapon submit |
| `GameRendererMixin` | Коэффициенты vanilla bob/hurt, FOV-поправка и optional world-image pass перед HUD |
| `ScreenMixin` | Цвет/fade in-world backdrop и opt-in wallpaper только PauseScreen |
| `ScreenEffectRendererMixin` | Fire quads и только pushed pose настоящего local totem activation model |
| `SkyRendererMixin` | Legacy palette / procedural dome / custom stars и celestial transforms, без замены world renderer |
| `FogRendererMixin` | Matching clear/fog color и только дополнительная density обычного air fog |
| `ContainerScreenMixin` | Декорация стандартных контейнеров и наблюдение локального click |
| `TitleScreenMixin` | Панель перед vanilla widgets и смещение original logo/splash |
| `TitleButtonMixin` | Skin только зарегистрированных кнопок текущего title screen; без input hooks |
| `PanoramaRendererMixin` | Цельная замена title panorama на Live Background; иначе два аргумента тематической cubemap camera |
| `LevelLoadingScreenMixin` | Только фон загрузки обычного мира; progress и portal screens неизменны |

`defaultRequire: 1` не маскирует пропавшие точки инъекции. `MixinContractTest` проверяет target methods, captured descriptors, shadow fields и INVOKE sites по байткоду фактического Minecraft 1.21.11. Это не полноценный запуск Mixin transformer и не проверка совместимости с другими модами.

## Ribbon extraction, Weapon Trails и Totem Motion

`TrailHistory` — core ring buffer точек или пар краёв: лимиты capacity/lifetime/path length, разрывы при телепорте и смене времени. `RibbonMesh` формирует immutable primitive-array snapshot со сглаженными camera-facing полосами и мягкими alpha edges. Player Trails хранит 64 samples; Fabric END_EXTRACTION прикрепляет mesh через RenderStateDataKey, AFTER_ENTITIES читает только snapshot и передаёт colored quads штатному MultiBufferSource. Draw не читает player/world/config. Particle styles сохраняют общий EffectEmitter. В Fabric rendering-v1 16.2.10 BEFORE_DEBUG_RENDER вызывается во время extraction, до renderContext.prepare: первый context пуст, позднее может быть stale. AFTER_ENTITIES выполняется в подготовленном main pass. Hooks дополнительно проверяют nullable state/matrices/consumers; OFF не читает draw context.

Weapon Trails сохраняет максимум 48 пар краёв на руку, не чаще 100 Hz, только во время местного swing. Узкий WrapOperation вокруг ItemInHandRenderer.renderItem устанавливает local weapon scope и всегда вызывает original/finally. ItemModelTrailMixin получает pose после actual ItemTransform.apply первого item layer; Viewmodel/Swing/resource-pack display transforms уже учтены. AUTO сохраняет edited legacy hand probes; MODEL использует верхнюю диагональ модели с Coverage. WeaponSweep разрывает историю между swing и скачками, RibbonMesh сглаживает probes без выхода за соседние границы. Mesh в camera space поступает в SubmitNodeCollector с identity pose. Iris active-pack guard приостанавливает новые геометрические modes; runtime compatibility пока не проверена.

TotemMotion — чистая функция от vanilla 40-tick progress. Totem Animation применяет pose перед существующим lighting/model submit внутри native push/pop; event, timer, sound и particles не изменяются. Нет нового renderer/mixin class или GPU resource lifetime.

ChoicePopup — модальный список внутри текущего editor, с ScrollPane и прежними NexButton. Он получает приоритет mouse/keyboard input, рисуется после controls на следующем GUI stratum и закрывается при rebuild/resize. SettingControls генерирует enum choices; editor использует тот же список для presets/sections. Apply по-прежнему отделён от выбора module preset. Focus/cursor search hotfix сохранён.

## Анимации и ограничения нагрузки

`Transition`, `Smoothing`, `EffectMath`, `SwingTimeline` и `SwingMotion` — маленькие тестируемые компоненты. SwingTimeline запускается только от принятого local vanilla swing, дубликаты tick не перезапускают его. SwingMotion переиспользует mutable pose на руку и отпускает carry предыдущего движения за 65 ms; render не обнаруживает новые cycles по vanilla progress. Контекст сбрасывается при смене player/world/item/arm/style и использовании предмета. GUI/swing используют монотонное время; particle simulation — ticks Minecraft с интерполяцией размера при рендере. Visual swing duration не записывает состояние атаки/cooldown.

- Общий emitter: 128 submissions/tick, 48 при Decreased; Minimal отключает эффекты.
- Новые частицы: `ParticleLimit(768)`; lifetime максимум 60 ticks. Legacy Classic имеет отдельный cap 256.
- Fireflies: отдельный cap 96, lifetime до 160 ticks, до 3 spawn attempts/tick. Проверяется только кандидат в уже загруженном chunk; нет сканирования мира/сущностей. Elytra Trails: до 16 samples/tick, без неограниченной истории позиции.
- Hit feedback ограничен одной эмиссией в 50 ms; sound — 60 ms.
- Trail: максимум 12 samples/tick, сброс на смене мира, invisible/spectator и телепорте более 3 blocks/tick.
- Hat: 40 segments, заранее рассчитанная окружность; максимум 80 quads с rim.
- Container: максимум 32 ghosts и 6 hover remnants; snapshots только на clicks и до 256 slots.
- Skybox: 512 dome quads, максимум 4000 star quads; field rebuild только на geometry edits. Отключённые atmosphere branches не вычисляют noise. Aurora/nebula скрываются по night/rain; максимум один meteor на интервал 6–40 секунд.
- Post-processing: одна full color copy, один основной pass; Compact максимум 11 scene samples/pixel с chromatic/edge softness. Wide добавляет четыре samples на pixel маленького highlight target (1/16 площади), затем основной pass максимум 7 scene + 9 highlight samples. Copy texture около `width × height × 4` bytes (31.6 MiB при 3840×2160); Wide target дополнительно примерно 2 MiB. Нет цепочки полноразмерных blur buffers, глубины/readback. OFF не делает GPU draws/copy; sky hooks выполняют быстрые guards.
- Live Background: title — одна GPU triangle без extra targets; другие поддерживаемые меню — quarter-area canvas + один native textured GUI quad. Canvas около 7.9 MiB при окне 3840×2160, создаётся только при первом входе/resize. До двух noise octaves только Nebula и bounded grid motes. Никаких particle collections/video assets/per-frame textures или CPU meshes. 96-byte ring и canvas закрываются при выходе/выключении; FPS на конкретном GPU проверяется вручную.
- Config не читается с диска в render/tick. Никаких сетевых операций модули не выполняют.

## Добавление модуля

```java
public final class ExampleModule extends VisualModule {
    private final DoubleSetting opacity = add(new DoubleSetting(
            "opacity", "Opacity", "Cosmetic alpha multiplier.", 0.8, 0, 1));

    public ExampleModule() {
        super("example", "Example", "A cosmetic example.", Category.INTERFACE);
        preset("Soft", "A subtle variant.", "opacity", 0.35);
    }
}
```

1. Объявить metadata/settings в модуле, при необходимости добавить `HudModule`/`EditableHud`.
2. Зарегистрировать его в `ClientModules`.
3. При необходимости подключить один поддерживаемый Fabric hook в bootstrap; проверять `enabled()`.
4. Чистую логику вынести в `core` и проверить unit tests; реальные definitions/presets уже охватывает catalog test.
5. GUI и config обнаружат настройки автоматически. Не добавлять индивидуальные branches для нового модуля в GUI.

## Viewmodel presets в 0.9

Viewmodel.apply получает Minecraft main arm из прежнего ItemInHandRenderer adapter. Новый mirror_layout (default false) опционально меняет знак X/yaw/roll для леворукого layout; прочие пользовательские числа и порядок transforms не меняются. PvP/Cinematic включают этот флаг и independent offhand; остальные preset recipes сохранены. Глобальный cinematic.json повторяет исправленный module recipe, соответствие проверяется headless тестом. ViewmodelPresetTest использует actual Minecraft 1.21.11 handheld.json / ItemTransform, vanilla rest hand placement и perspective clip coordinates; обнаруживает оба старых off-screen recipes и проверяет новые на нескольких projections. GPU/window/registries не запускаются.

## Дополнения 1.0: снаряды и подборы

`ProjectileTrailsModule` использует Fabric `ClientEntityEvents.ENTITY_LOAD / ENTITY_UNLOAD` и end-client-tick. Включение в уже загруженном мире делает один ограниченный bootstrap (до 4096 candidates), дальнейшая работа идёт только по максимум 32 tracked projectile references. `MotionTrail` хранит один предыдущий/текущий отрезок, не историю. На tick проверяются тип, движение, distance, известный owner при Own only и native line of sight; исключение разрывает отрезок. Teleport более восьми blocks не соединяется. До восьми samples на projectile и 48 суммарно за tick проходят через общий `EffectEmitter`; Minimal particles выключает emission. Стандартные частицы отвечают за depth test и lifetime.

`ClientPacketListenerMixin.handleTakeItemEntity` наблюдает vanilla pickup после client-thread handoff, до shrink/remove исходного ItemEntity. Только packet с ID локального игрока и уже известным ItemEntity передаётся `PickupHudModule`. Нет записи stack/slot state, packet sends или cancellation. `PickupFeed<T>` объединяет matching payloads, ограничивает шесть строк / count 9999 и удаляет expired notices. Payload содержит копию item count=1 и cached localized name; labels обновляются по revision / изменению options, без item copies в render path.

`PickupHudModule` наследует TextHudModule / EditableHud и общий HudDispatcher, поэтому GUI, drag/anchors/scale/config/profiles не требуют отдельных branches. Время берётся из game ticks + partial tick; pause замораживает его. Fade применяется к text/accent, native item icons сохраняют штатную непрозрачность. Notices сбрасываются на OFF / world change и не сериализуются; сохраняются только настройки. Эти caps и config/profile round trips проверяются headless tests, внешний вид — пользователем в Minecraft.
