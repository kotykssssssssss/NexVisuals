# Sound Feedback 0.7

Обновление существующей системы NexVisuals для **Minecraft 1.21.11**. GUI, profiles, настройки, renderer и прежние визуальные модули сохранены. Ни одно игровое действие не автоматизируется.

## Hit Sounds

**Combat → Hit Sounds**: включите модуль, выберите Sound или module preset → Apply. `Preview sound` воспроизводит выбранный cue, в том числе в меню без мира.

| Sound / готовый preset | Ресурс в локальном JAR |
| --- | --- |
| Cricket / Cricket Bat | `cricket-bat-hitting-sound.mp3` → `hit_cricket.ogg` |
| Hit 2 | `hitsound_2.mp3` → `hit_2.ogg` |
| Critical | `critical-hit-sounds-effect.mp3` → `hit_critical.ogg` |
| Hitmarker | `hitmarker_2.mp3` → `hitmarker.ogg` |
| Alpha Damage | `minecraft-alpha-damage-sound-effect.mp3` → `alpha_damage.ogg` |
| Osu | `osu-hit-sound.mp3` → `osu.ogg` |

Прежние **Soft / Click / Pop / Bell / Metallic / Arcade** сохранены как ссылки на установленные vanilla events. Settings: volume 0–1, pitch 0.5–2, pitch variation 0–0.4; все шесть новых module recipes используют pitch 1, variation 0 и volume 0.4. Название Critical — выбор звука, не автоматическое определение critical damage.

Feedback возникает при локальной попытке атаки по текущей видимой сущности через прежний Fabric callback. Это **не подтверждение урона сервером**: защищённая/неуязвимая цель также может вызвать cue. Reach/cooldown/attack state/packets не меняются.

`Quieter vanilla attacks` включено по умолчанию внутри модуля; `Vanilla attack volume` = **0.25**. После собственной попытки атаки приглушаются только positional PLAYERS events `entity.player.attack.strong/weak/crit/knockback/sweep/nodamage`, возникшие в радиусе 1 block от игрока за 450 ms. **Hurt/death**, breaking, footsteps, music и общие sound sliders не изменяются. Setting 1, выключение quieter/module или custom Volume 0 сохраняет native gain.

В самом sound instance нет owner ID: одновременно возникший чужой attack cue совсем рядом может попасть в это окно. Удалённые звуки вне окна не меняются. Это сознательное ограничение узкого acoustic filter, без перехвата/изменения combat packets. Subtitle/category/instance identity сохраняются.

Максимум один одновременно проигрываемый пользовательский clip на модуль; следующий обрывает только его, чтобы длинные записи не складывались при быстрых кликах. Hit cue ограничен интервалом 60 ms. Короткие прежние vanilla voices сохраняют прежнее поведение. Cue уважает Minecraft master sound volume; in-world native attacks дополнительно зависят от Players volume.

## Totem Pop Sounds

**Combat → Totem Pop Sounds** — отдельный модуль. Использует только настоящий client entity status **35**, когда entity — собственный игрок. Чужие тотемы не отслеживаются и звучат обычно. Существующий **Totem Echo** остаётся независимым.

Sounds: **Vanilla, Chime, Bell, Arcade, Soft** и все шесть предоставленных clips. Готовые module presets: **Crystal Pop / Bell Pop / Arcade Pop / Soft Pop / Impact Pop / Classic Pop / Vanilla**. Impact использует Hit 2, Classic — Alpha Damage. Любую запись можно выбрать напрямую в Sound.

Параметры: volume, pitch, variation, **Vanilla underlay volume** (по умолчанию 0.2). Custom cue играет вместе с более тихим native cue; underlay 0 оставляет только выбранный звук, 1 сохраняет native громкость. При custom Volume 0 native cue сохраняется. Vanilla voice меняет исходный gain/pitch, без второй копии. Preview играет только выбранный cue, не underlay и не событие тотема. Частицы, activation item animation, burning/health и серверная логика не изменяются.

Totem cue ограничен 100 ms и одним собственным SoundInstance; следующий cue/OFF останавливает только этот instance. Другие Minecraft sounds не останавливаются.

## Totem Tracker HUD

**HUD → Totem Tracker** считает собственные реальные срабатывания при включённом HUD и показывает время после последнего. Это отличается от **Item Counter**, который считает предметы в инвентаре.

Presets: **Timeline** (count + time), **Counter** (count), **Minimal** (короткая подпись без панели). Общие text/background colors, alpha, scale, anchor и нормализованная drag-позиция из прежнего HUD Editor. `Reset counter` очищает только счётчик. `Reset on world change` по умолчанию включён, включая смену измерения; можно отключить.

Count и последний timestamp — **данные текущей сессии**, не сохраняются в config/profiles. Настройки внешнего вида/позиции сохраняются. Totem Pop Sounds/Totem Echo могут быть выключены: Tracker не зависит от них. Preview звука не увеличивает count. Выключенный Tracker не считает события задним числом.

## Подготовка и сборка пользовательских clips

Готовый JAR этого этапа уже содержит шесть предоставленных clips. Для установки достаточно JAR; отдельно FFmpeg/resource pack/MP3 не нужны.

Tracked repository содержит только converter и definitions. Подготовленные clips/manifest находятся в ignored `.tools/user-sounds/`. Обычный `clean build` **не включает их**: сохраняет новые sound IDs через шесть vanilla event fallbacks. Это позволяет воспроизводимую сборку без локальных MP3 и не объявляет чужие записи свободно распространяемыми. Их авторство/лицензии не установлены; см. [ASSETS](../ASSETS.md).

Для повторной локальной подготовки нужен Python 3 и доступный FFmpeg/ffprobe с libvorbis:

```powershell
python tools/prepare_user_sounds.py --input-dir E:\Downloads --ffmpeg .tools/audio-converter/ffmpeg-9.0.2-essentials_build/bin/ffmpeg.exe
.\gradlew.bat clean build -PuserSoundResources=.tools/user-sounds
```

Пути input/FFmpeg должны соответствовать реальным файлам. Converter ничего не скачивает, originals не изменяет; output разрешён только внутри NexVisuals. При смене audio build mode используйте `clean`.

Conversion: mono 44100 Hz **OGG Vorbis** q4, максимум 5 секунд/clip и 8 MiB/source, silence trim с коротким pad, peak target 0.75/gain cap 4, 3 ms edge fade. MP3 metadata удаляется. Реальные durations: Cricket 0.194 s, Hit 2 0.604 s, Critical 1.000 s, Hitmarker 0.064 s, Alpha Damage 0.657 s, Osu 0.165 s. Каждый OGG проверяется ffprobe и полным decode-to-null, повторная conversion дала те же hashes. Субъективная громкость/качество оцениваются в игре пользователем, не проверены на слух здесь.

Manifest `nexvisuals/user-audio.json` хранит basenames, SHA-256 original/OGG, duration, codec parameters и origin notice. Нет абсолютных путей/личных данных. Gradle проверяет шесть definitions/assets в production JAR; tests проверяют metadata/hashes/config/legacy settings/profiles. FFmpeg не входит в JAR/обязательные зависимости.

Источники формата/toolchain: [Fabric Sounds 1.21.11](https://docs.fabricmc.net/1.21.11/develop/sounds/using-sounds), [официальные FFmpeg downloads](https://ffmpeg.org/download.html). Локальный Gyan FFmpeg 9.0.2 ZIP проверен по опубликованному SHA-256 `60f467265b1e312373dbcd92200c2618a74850f98d3d078e94296bb3fa2047ba`.

## Ручная проверка

Minecraft не запускался, звуки не прослушивались. Установите новый production JAR вместо старого и проверьте:

1. Hit Sounds: Preview каждого из шести clips и прежних vanilla voices; попытка атаки, быстрые повторные атаки, volume/pitch/variation. Critical voice не должна переключаться сама.
2. Quieter vanilla attacks: gain 0/0.25/1, quieter OFF, Hit Sounds OFF и Volume 0. Hurt/death, nearby other players и Music/Blocks/Players/master volume sliders; остальные sounds должны сохраняться с указанным выше ограничением близкого совпадения.
3. Totem Pop Sounds: настоящий собственный pop, Crystal/Impact/Classic/Vanilla; underlay 0/0.2/1, custom Volume 0, module OFF. Vanilla voice — один исходный звук. Чужой pop звучит как прежде; частицы/item animation остаются.
4. Totem Tracker: Preview не считает; настоящий собственный pop увеличивает count один раз даже при выключенных Sounds/Echo. Timer, Reset counter, OFF/ON, смена мира/измерения и reset option. Drag/scale/anchor при resize.
5. Config/profile/restart: voices, mixer, enabled и HUD placement восстанавливаются; session counter сбрасывается. Старые presets/menu/sky/мини-HUD/Fire/Shield работают как прежде.
6. Resource reload (F3+T), performance, Sodium/Iris при их наличии. Совместимость этой сборки с ними не подтверждена runtime проверкой.
