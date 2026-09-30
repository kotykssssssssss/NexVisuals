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

`BuiltinProfile` перечисляет три bundled global recipes, которые `ProfileManager.applyBuiltin` загружает через тот же ConfigManager. Путь задаётся только enum, а не текстом пользователя. Catalog tests проверяют, что все module/setting IDs рецептов существуют, и что итоговые конфигурации различны и сохраняются.

`SettingControls` — единая фабрика controls; `NexVisualsScreen` автоматически выводит настройки, selector пресетов и actions. Новому модулю не нужен собственный settings screen. Для цветов есть ARGB-поле и шесть быстрых swatches, сохраняющих alpha.

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

`SkyboxModule` объявляет обычные typed settings / groups / presets, получает immutable `Frame` при vanilla sky extraction. `SkyMath` вычисляет плавные палитры по реальному sun angle; направление/фаза celestial objects не подменяются. `SkyboxRenderer` заменяет только sky disc собственной sphere mesh (32×16 quads), а Custom Stars — отдельным bounded field. `StarField` создаёт детерминированное распределение unit-sphere; GPU geometry меняется только при правке количества/размера. Цвета, day/night weights, twinkle и atmosphere time передаются через UBO. Nebula, aurora и один meteor на интервал — аналитические fragment layers, без сущностей, particles/history или доступа к скрытым объектам. Vanilla clouds/precipitation/sunrise disc сохраняются.

`SkyRendererMixin` — узкие adapters existing sky methods, scale constants X/Z и color modulator sun/moon/stars. У size hooks `require=2` соответствует двум scalar constants каждой vanilla scale. `FogRendererMixin` меняет computed atmospheric color перед UBO upload, чтобы clear color совпадал с fog. Все шесть дистанций делятся на density 1–3, без расширения видимости. Skybox применим только к Overworld air и уступает Minecraft vision/fluid rules; старый Sky Palette остаётся fallback при неактивном новом модуле.

`PostProcessingModule` владеет settings/presets и guard, `PostProcessingRenderer` — одним переиспользуемым color-only `TextureTarget`. После vanilla world/entity post chains, перед HUD/UI: main color копируется в target, fullscreen triangle рисует grading и небольшие эффекты обратно в main color. Depth не копируется/не меняется, readback отсутствует. Resize пересоздаёт только copy target. Цветовые формулы имеют независимый CPU reference `ColorGrade`; его tests не подтверждают фактическое изображение GPU. Draw/shader compile failure закрывает optional resources, пишет одну error и требует OFF/ON для retry.

Pipelines строятся через `RenderPipeline` snippets и не регистрируются как обязательные vanilla shaders: первый draw проверяет `GpuDevice.precompilePipeline().isValid()`. Это позволяет fallback при недоступном custom shader, без принудительного падения всего vanilla resource reload. Native uniform rings: SkyConfig 144 bytes, StarConfig 48 bytes, VisualConfig 144 bytes. Выключение и shutdown освобождают buffers/targets; cached meshes не перестраиваются каждый кадр. При resource reload используется shader cache Minecraft; успешность reload/GPU output проверяется вручную.

`RenderCompatibility` обнаруживает Iris через Fabric Loader и один раз связывает **публичный** `IrisApi.isShaderPackInUse` с MethodHandle. В кадре нет reflection search. Активный shader pack или недоступный API блокирует оба новых rendering paths с сообщением в generic GUI; settings остаются сохранёнными. Post target освобождается при блокировке. При установленном Iris без pack новые paths допускаются, но практическая совместимость не проверена. Нет управления private shader options или скачивания packs.

## Container visuals

Одна `ContainerVisualState` принадлежит одному `AbstractContainerScreen`. Общие hooks украшают panel и active slots, наблюдают `slotClicked` и рисуют ghosts после обычного содержимого. Они не отменяют clicks, не меняют slots/packets и не задерживают close.

Для `QUICK_MOVE` до/после обычного клиентского действия снимаются counts **одного точного item + components**. `TransferMatcher` возвращает destinations только при полном сохранении количества, отсутствии других уменьшающихся slots и не более 8 destinations. Это наблюдаемое локальное prediction, не подтверждение сервера. При неоднозначном/асинхронном результате — только source pulse. Pickup/right click/drop/quick craft дают slot feedback без выдуманного маршрута.

Opening Scale/Slide/Fade относится к декоративной рамке; интерактивные slots неподвижны. Это сознательное ограничение для правильной геометрии мыши. Иконки ghosts исчезают уменьшением; общий безопасный alpha multiplier для произвольных GUI items не добавлялся.

## Console menu

`ConsoleMenuModule` хранит typed settings и четыре module presets, использует общий config/profile flow. `ScreenEvents.AFTER_INIT` связывает текущий `TitleScreen` с `ConsoleTitleState`. Последний сохраняет original vanilla button instances и их actions/active flags/tooltips/narration, меняя bounds и skin. Временное состояние освобождается при удалении экрана или выключении модуля, восстанавливается через обычный init. Общий settings screen можно открыть из title screen; HUD Editor там неактивен без мира.

`ConsoleLayout` рассчитывает panel/logo/button geometry в GUI pixels; `MenuMotion` — углы только существующего menu cubemap. `MenuClock` использует монотонное время и замораживает движение без catch-up при паузе. После долгого отсутствия меню delta ограничена 250 ms. `ConsoleTheme` рисует beveled pixel-панели и максимум 18 фоновых motes обычными `GuiGraphics` primitives. Новый framebuffer, shader pipeline или копия vanilla assets не создаются. Меню остаётся title screen Minecraft, сетевые действия и ограничения аккаунта не подменяются.

Тема загрузки меняет только background обычного `LevelLoadingScreen.Reason.OTHER`. Progress tracker, chunk map, narration и close/tick принадлежат Minecraft. Начальный resource-loading overlay и порталы не заменяются.

## Mixins: зачем они нужны

| Адаптер | Узкая ответственность |
| --- | --- |
| `ItemInHandRendererMixin` | Трансформация внутри pushed hand matrix; замена только обычного `swingArm` |
| `GameRendererMixin` | Коэффициенты vanilla bob/hurt, FOV-поправка и optional world-image pass перед HUD |
| `ScreenMixin` | Цвет/fade обычного in-world backdrop |
| `ScreenEffectRendererMixin` | Только first-person fire quads: матрица, alpha, видимость |
| `SkyRendererMixin` | Legacy palette / procedural dome / custom stars и celestial transforms, без замены world renderer |
| `FogRendererMixin` | Matching clear/fog color и только дополнительная density обычного air fog |
| `ContainerScreenMixin` | Декорация стандартных контейнеров и наблюдение локального click |
| `TitleScreenMixin` | Панель перед vanilla widgets и смещение original logo/splash |
| `TitleButtonMixin` | Skin только зарегистрированных кнопок текущего title screen; без input hooks |
| `PanoramaRendererMixin` | Два аргумента menu cubemap camera при активной теме, без world camera |
| `LevelLoadingScreenMixin` | Только фон загрузки обычного мира; progress и portal screens неизменны |

`defaultRequire: 1` не маскирует пропавшие точки инъекции. `MixinContractTest` проверяет target methods, captured descriptors, shadow fields и INVOKE sites по байткоду фактического Minecraft 1.21.11. Это не полноценный запуск Mixin transformer и не проверка совместимости с другими модами.

## Анимации и ограничения нагрузки

`Transition`, `Smoothing`, `EffectMath` и `SwingTimeline` — маленькие тестируемые компоненты. GUI/swing используют монотонное время; particle simulation — ticks Minecraft с интерполяцией размера при рендере. Visual swing duration не записывает состояние атаки/cooldown.

- Общий emitter: 128 submissions/tick, 48 при Decreased; Minimal отключает эффекты.
- Новые частицы: `ParticleLimit(768)`; lifetime максимум 60 ticks. Legacy Classic имеет отдельный cap 256.
- Fireflies: отдельный cap 96, lifetime до 160 ticks, до 3 spawn attempts/tick. Проверяется только кандидат в уже загруженном chunk; нет сканирования мира/сущностей. Elytra Trails: до 16 samples/tick, без неограниченной истории позиции.
- Hit feedback ограничен одной эмиссией в 50 ms; sound — 60 ms.
- Trail: максимум 12 samples/tick, сброс на смене мира, invisible/spectator и телепорте более 3 blocks/tick.
- Hat: 40 segments, заранее рассчитанная окружность; максимум 80 quads с rim.
- Container: максимум 32 ghosts и 6 hover remnants; snapshots только на clicks и до 256 slots.
- Skybox: 512 dome quads, максимум 4000 star quads; field rebuild только на geometry edits. Отключённые atmosphere branches не вычисляют noise. Aurora/nebula скрываются по night/rain; максимум один meteor на интервал 6–40 секунд.
- Post-processing: одна color copy и один fullscreen pass; максимум 7 scene samples/pixel при одновременном glow/chromatic. Без цепочки blur targets; copy texture требует около `width × height × 4` bytes (примерно 31.6 MiB при 3840×2160), плюс маленький uniform ring. При OFF GPU draws/copy отсутствуют; sky hooks выполняют только быстрые guards.
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
