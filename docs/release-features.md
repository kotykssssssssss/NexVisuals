# Модули и сборки 1.0.0

Этот этап расширяет существующий NexVisuals для Minecraft **1.21.11**; старые module IDs / config schema / визуальные функции сохранены. Оба новых модуля выключены по умолчанию и используют общие settings, presets, reset и profiles.

## Projectile Trails

**Particles → Projectile Trails → выбрать preset → Apply → Enabled.**

| Preset | Форма и поведение |
| --- | --- |
| Comet | Мягкий cyan/violet поток круглых частиц, уменьшающихся вдоль прошедшего пути |
| Embers | Короткие тёплые искры, с вращением и небольшим падением |
| Pearl Halos | Редкие расширяющиеся кольца; по умолчанию только жемчуг |

`Projectiles`: Ender pearls, Arrows / tridents, Other thrown items (snowballs / eggs / potions), Own projectiles only и Distance 8–64 blocks. Own only требует известного клиенту owner; неизвестные owners пропускаются. `Appearance`: Style, два ARGB colors, Size .025–.2, Density 1–12 samples/block, Lifetime 4–40 ticks. Primary/secondary alpha меняет opacity; Apply сбрасывает recipe перед применением, затем числа можно свободно править.

Только прошлые позиции реально загруженных снарядов. Нет prediction, destination markers или скрытой информации. Native line-of-sight / invisible / distance / stationary guards прекращают emission; обычная particle depth скрывает эффект за геометрией. Сегмент не соединяется через hidden interval / teleport. Движение позади камеры само по себе не запрещено: это cosmetics с обычной глубиной, не overlay.

Ограничения: максимум 32 tracked projectiles, 8 samples/projectile/tick, 48 submissions/tick и общий emitter budget/cap. При Minecraft Minimal particles эффект выключен; при Decreased действует общий меньший budget. В особенно насыщенной сцене часть следов пропускается. OFF / world change очищает references; частицы не сохраняются на диск.

## Pickup HUD

**HUD → Pickup HUD → Stacked / Compact / Minimal → Apply → Enabled.**

- **Stacked:** item icons / names и Slide.
- **Compact:** маленькие count-only cards с icons и Pop.
- **Minimal:** text / accent без icons/background, Fade.

Общие controls: Position/Anchor, Scale, Text/Background colors, shadow, rounding. `Notifications`: Animation, Duration .75–8 seconds, Visible rows 1–6, Item icons / names, Text width 48–240 GUI pixels, Accent ARGB. HUD Editor позволяет drag / scale / toggle как для остальных элементов; preview не создаёт вымышленные pickup events.

Только свои server-confirmed ItemEntity pickup packets, когда source item существует в client level. Pickup другого игрока не показывается; удалённые/неизвестные entities не угадываются. Matching item+components объединяются и обновляют время, самые новые notices сверху. Amount ограничен 9999 и отображает `+9999+` у верхнего лимита. Максимум шесть payloads, lifetime в game ticks; pause замораживает анимацию. Обычные inventory slot changes, crafting / commands / experience orbs не являются ItemEntity pickups и не создают notices.

Fade действует на подписи/акценты; icon рисует vanilla item renderer с его обычной opacity. Pop масштабирует строку. OFF / смена мира очищает feed; история не переносится через restart. Сохраняются настройки и placement.

## Аудио и production JAR

Личный вариант `build/libs/nexvisuals-1.0.0.jar` содержит все шесть ранее предоставленных клипов. Публичный `build/release/nexvisuals-1.0.0.jar` не содержит MP3/OGG/manifest: прежние sound IDs используют installed Minecraft event fallbacks. Пункты меню сохраняются; Hit Sounds / Totem Pop Sounds показывают actual audio build mode. Не устанавливать оба JAR одновременно.

Сборка (из корня проекта с JDK 21):

```powershell
# Public audio mode, без сторонних клипов:
.\gradlew.bat clean build
# Сохранить remapped build/libs JAR вне build/ до следующего clean.
# Personal audio mode, из уже подготовленных локальных ignored resources:
.\gradlew.bat clean build -PuserSoundResources=.tools/user-sounds
```

Обычный Gradle output обоих режимов — `build/libs/`; подготовленный public artifact отдельно скопирован в `build/release/`. Это не вторая автоматическая Gradle задача. Происхождение/права и converter — [ASSETS](../ASSETS.md), [Sound Feedback](sound-feedback.md). MIT относится к коду проекта, не к присланным клипам.

## Проверить вручную

1. Каждый Projectile Trails preset с луком / трезубцем / жемчугом; colors/size/density/lifetime, Own only, препятствия и distant projectiles. Нет мостов после teleport; OFF / Minimal particles прекращают emission.
2. Собрать stack items, несколько разных и одинаковые с разными components; подобрать предмет другим игроком рядом — чужого notice быть не должно. Проверить expiration / pause / отключение / вход в другой мир.
3. Stacked / Compact / Minimal, Slide / Pop / Fade, drag/scale/anchors в HUD Editor и resize.
4. Save / Apply / Rename profiles, Reset, restart; старый config не удалять.
5. На личном JAR Preview каждого из шести клипов Hit/Totem Sounds, quieter attacks; на public JAR проверить vanilla fallback и статус сборки.
6. Прежние search/scroll/tooltips, Viewmodel PvP/Cinematic, Swing/Weapon Trails, Mini Fire/Shield, Skybox/Live Background, затем Sodium/Iris и performance.

Новые visual/runtime-функции не проверены запуском игры. Реальные автоматические результаты — [validation](validation.md); готовое описание для GitHub — [RELEASE_NOTES](../RELEASE_NOTES.md).
