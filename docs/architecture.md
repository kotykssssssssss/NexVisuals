# Архитектура NexVisuals

Проект остаётся клиентским Fabric-модом для **Minecraft 1.21.11**. `src/main` — обычная Java-логика и ресурсы, `src/client` — интеграция с игрой. Серверного entrypoint нет.

## Регистрация и жизненный цикл

`ClientModules` создаёт реальный каталог модулей и общие сервисы. Он не открывает окна и не регистрирует hooks. `NexVisualsClient` связывает каталог с Fabric, загружает конфигурацию, регистрирует keybind и сохранение. Узкие Mixins получают те же экземпляры через entrypoint.

`VisualModule` содержит ID, название, описание, категорию, настройки, пресеты и необязательные команды редактора. Включение проходит через `onEnable`, выключение — через `onDisable`. Все модули по умолчанию выключены. `ModuleRegistry` отвергает повторяющиеся ID.

Модуль не должен загружать файлы или регистрировать новый event listener при каждом включении. Hooks устанавливаются один раз и проверяют текущие значения в памяти.

## Settings и presets

`Setting<T>` хранит типизированные default/current значения, ID, label и описание. Реализации поддерживают boolean, int, double, диапазон, enum, ARGB, текст и keybind. Числа clamp-ятся; NaN/Infinity и ошибочные типы отвергаются.

`ModulePreset` — набор обычных JSON-значений настроек. Применение явное: сначала defaults текущего модуля, затем значения пресета. Состояние enabled не меняется. Если в пресете ошибка, все прежние настройки восстанавливаются. Это позволяет применить стиль, а затем свободно менять sliders; загрузка config не запускает preset заново.

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
- `Draw` предоставляет маленький слой GUI-примитивов и clipping поверх `GuiGraphics`. Собственного OpenGL/framebuffer engine нет.
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

## Container visuals

Одна `ContainerVisualState` принадлежит одному `AbstractContainerScreen`. Общие hooks украшают panel и active slots, наблюдают `slotClicked` и рисуют ghosts после обычного содержимого. Они не отменяют clicks, не меняют slots/packets и не задерживают close.

Для `QUICK_MOVE` до/после обычного клиентского действия снимаются counts **одного точного item + components**. `TransferMatcher` возвращает destinations только при полном сохранении количества, отсутствии других уменьшающихся slots и не более 8 destinations. Это наблюдаемое локальное prediction, не подтверждение сервера. При неоднозначном/асинхронном результате — только source pulse. Pickup/right click/drop/quick craft дают slot feedback без выдуманного маршрута.

Opening Scale/Slide/Fade относится к декоративной рамке; интерактивные slots неподвижны. Это сознательное ограничение для правильной геометрии мыши. Иконки ghosts исчезают уменьшением; общий безопасный alpha multiplier для произвольных GUI items не добавлялся.

## Mixins: зачем они нужны

| Адаптер | Узкая ответственность |
| --- | --- |
| `ItemInHandRendererMixin` | Трансформация внутри pushed hand matrix; замена только обычного `swingArm` |
| `GameRendererMixin` | Коэффициенты vanilla bob/hurt и косметическая FOV-поправка |
| `ScreenMixin` | Цвет/fade обычного in-world backdrop |
| `ScreenEffectRendererMixin` | Только first-person fire quads: матрица, alpha, видимость |
| `SkyRendererMixin` | Цветовые поля extracted `SkyRenderState`, без замены world renderer |
| `ContainerScreenMixin` | Декорация стандартных контейнеров и наблюдение локального click |

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
