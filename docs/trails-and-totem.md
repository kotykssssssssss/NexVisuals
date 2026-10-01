# Trails, Totem Animation и списки меню — 0.8

Обновление для **Minecraft 1.21.11**, Java 21, Fabric. Старые настройки и particle styles не удалены. Weapon Trails и Totem Animation по умолчанию выключены; прежний Player Trails продолжает использовать SILK, пока пользователь не выберет другой стиль.

**Hotfix 0.8.1:** устранён first-frame crash. В закреплённом Fabric API BEFORE_DEBUG_RENDER вызывается раньше подготовки draw context; вместо него используется AFTER_ENTITIES. Null state/matrices/buffers безопасно пропускаются.

## Player Trails: непрерывные ленты

Откройте **Particles → Player Trails**. Presets теперь выбираются раскрывающимся списком, затем нажмите **Apply**.

| Preset | Результат |
| --- | --- |
| Ribbon | Широкая cyan лента с violet хвостом, мягкими краями и сужением |
| Twin Flow | Две параллельные mint/blue ленты |
| Light Line | Тонкая gold/pink линия с коротким хвостом |
| Silk / Ember / Prism / Ripples | Прежние particle styles, без изменения их rendering |

Новые режимы Style: **RIBBON / DUAL / LINE**. Primary/Secondary — ARGB цветов свежей/старой части, Lifetime — ticks, Width — ширина, Height — высота над ногами. Fade управляет затуханием alpha. Раздел **Ribbon geometry** содержит Maximum ribbon length (пройденная длина в блоках), Tail taper и Smoothness 1–3. Density относится только к particle modes. Линия использует 35% заданной ширины.

История содержит максимум **64 tick samples**. Старые точки удаляются по lifetime и длине пути. Остановка не продлевает жизнь хвоста. Движение более 3 блоков между samples, смена мира/стиля, OFF, spectator/invisible или исключённая перспектива сбрасывают историю. По умолчанию след показывается только в third person. Minecraft Particles: Minimal скрывает эффект, Decreased ограничивает subdivisions до 1.

Геометрия сглажена Catmull–Rom; полосы ориентируются к камере и имеют прозрачные наружные края. Fabric **END_EXTRACTION** публикует неизменяемый camera-relative mesh в `FabricRenderState`, **AFTER_ENTITIES** отправляет его в штатный `RenderTypes.debugQuads`. Depth test, alpha blending и сортировку выполняет Minecraft. Лента не просвечивает сквозь непрозрачные блоки. Это светлая цветная геометрия, без отдельного bloom/framebuffer/shader engine. Край прозрачных блоков и воды требует ручной проверки из-за порядка мировых прозрачных слоёв.

Максимум **1134 quads** для Dual с smoothness 3; обычный Ribbon при default smoothness 2 — максимум 378 quads. Mesh занимает максимум примерно 71 KiB плюс небольшие рабочие массивы на extraction. Никакой бесконечной коллекции, textures/framebuffers или объектов на каждый vertex нет. Это верхние границы, не результаты FPS benchmarks.

## Weapon Trails

**Обновление 0.8.2:** выравнивание и playback переработаны. Актуальные настройки, AUTO/model space, новые лимиты и checklist — в [swing-and-weapon-trails](swing-and-weapon-trails.md). Следующие абзацы описывают первоначальную реализацию 0.8.0.

**Viewmodel → Weapon Trails**. Короткий sweep в первом лице для собственного меча или топора. Цветовые presets **Prism / Ember / Frost** различаются палитрой и lifetime; это три оформления одного ribbon motion, а не три независимых animation engines.

Настройки: два ARGB цвета, Lifetime 40–350 ms, Include off hand. **Blade alignment** задаёт base/tip X/Y/Z. Эти точки находятся в hand-render space до item display transform. Значения по умолчанию — приблизительное выравнивание обычного оружия; их нужно откалибровать, если модель/пак/рука выглядит иначе. Точки не являются hitboxes, точная геометрия текстуры предмета не анализируется.

Samples берутся из финальной hand matrix перед `renderItem`: учитываются Viewmodel, Item Swing и vanilla transforms. Предметы не получают новые атаки/cooldowns. Запись происходит только во время локального vanilla/custom swing, затем след затухает. Eating/charging, auto-spin, невидимость/spectator, third person и другие items исключены. Camera-space history хранит максимум **48 пар краёв на руку**, sampling — не чаще 100 Hz. Geometry snapshots неизменяемы и поступают в существующую `SubmitNodeCollector` queue с identity pose. Максимум 141 quads на руку; FPS влияет на детализацию samples, а не на lifetime.

Оба геометрических trail renderer консервативно приостанавливаются при активном Iris shader pack или недоступной проверке публичного Iris API. Particle modes Player Trails сохраняют прежний путь. Runtime совместимость с Sodium/Iris не заявляется без пользовательского тестирования.

## Totem Animation

**Viewmodel → Totem Animation** меняет только экранную модель настоящего локального срабатывания тотема.

| Preset | Движение |
| --- | --- |
| Classic | Обычная vanilla анимация и полный размер |
| Compact | Vanilla motion, размер 40%, небольшой offset |
| Float | Мягкое появление/исчезновение, небольшой подъём и наклон |
| Spin | Поворот вокруг Y с плавной scale envelope |
| Pop | Короткое расширение, движение вперёд и лёгкий roll |

Можно менять Size, X/Y offset, pitch/yaw/roll; для custom motion дополнительно Distance, Motion strength, Spin turns и Easing. Для VANILLA режима дополнительные distance/motion/turns не применяются. Новый default — vanilla motion с размером 65%.

`ScreenEffectRendererMixin` корректирует только pushed pose перед vanilla lighting/model submit. Custom poses рассчитывает чистый `TotemMotion` по существующему **40-tick progress**. Не изменяются timer, packets, event, частицы, игровые свойства тотема, Totem Echo, Totem Pop Sounds или Tracker. True opacity и сокращение длительности не добавлены: alpha всех возможных item layers требует отдельной интеграции.

## Раскрывающиеся списки

В прежнем NexVisuals GUI раскрываются **module presets, sections и все EnumSetting**. До восьми строк видимы одновременно; длинные списки прокручиваются. Up/Down, Tab/Shift+Tab, Home/End, Page Up/Down и Enter/Space работают внутри списка. Esc или click вне списка закрывает его и не активирует control позади. Enum choice применяется сразу, module preset — только через Apply. Изменение списка не закрывает сам editor и не создаёт отдельную систему обоев.

Сохранён hotfix поиска: focus/cursor восстанавливаются после native `rebuildWidgets`, поэтому фильтрация не должна прерывать ввод. Все новые настройки используют общие reset/config/profiles, schemaVersion остаётся 1. Неизвестные поля игнорируются, отсутствующие новые поля получают defaults.

## Ручная проверка

Minecraft в этом этапе **не запускался**. Java/tests/Mixin bytecode contracts не подтверждают GPU draw или интерактивное поведение.

1. Player Trails: third person, Ribbon/Twin Flow/Light Line; ходьба, повороты, спринт, прыжок, остановка, телепорт, dimension change. Проверьте taper/fade/width/length, глубину у стен/воды, Minimal/Decreased и возврат Silk/Ember. OFF должен сразу убрать геометрию.
2. Weapon Trails: меч/топор, vanilla и custom Swing, Viewmodel offsets/scale, right/left main arm и offhand. Настройте blade probes по модели; проверьте отсутствие следа при idle/eating/зарядке и OFF.
3. Totem: настоящее срабатывание с каждым preset; размер/offset/rotation/easing, полный vanilla timer. Sounds/Echo/Tracker работают совместно. OFF возвращает обычную модель.
4. GUI: click и keyboard выбор enum/section/preset, длинный список с scroll, Esc/click outside, маленькое окно. Preset не применяется до Apply. Поиск принимает несколько символов подряд и Backspace после rebuild/resize.
5. Config/profiles: save/load, restart, Reset settings, прежний config. Проверьте прежние Fire/Shield/Skybox/Live Background/Hit Sounds.
6. FPS/resource reload и optional Sodium/Iris: native renderer, Sodium, Iris без pack, Iris с pack (геометрические trails должны приостановиться и восстановиться после отключения pack). Проверьте логи на ошибки.
