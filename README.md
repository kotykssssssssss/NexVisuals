# NexVisuals

Самостоятельный клиентский Fabric-мод визуальной кастомизации для **Minecraft Java Edition 1.21.11**. Не является частью NexLauncher и не использует его код, сборку или данные.

Проект построен вокруг независимых визуальных модулей с типизированными настройками. Здесь нет боевой автоматизации, скрытой информации, ESP/X-Ray, сетевых эксплойтов, телеметрии или загрузки удалённого исполняемого содержимого.

## Разработка

Нужен **JDK 21**. Установленный системный Gradle не требуется: используйте включённый Wrapper. Первая сборка скачивает Gradle, Minecraft, mappings и зависимости из официальных репозиториев.

Windows PowerShell (укажите фактический путь к своему JDK 21):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Linux/macOS:

```sh
export JAVA_HOME=/path/to/jdk-21
export GRADLE_USER_HOME="$PWD/.gradle-user-home"
sh gradlew clean build
sh gradlew runClient
```

Запускайте команды из директории NexVisuals. `JAVA_HOME` и `GRADLE_USER_HOME` в этих примерах меняются только для текущей оболочки. Minecraft development client использует локальную папку `run/`; пользовательская установка Minecraft не изменяется. Проверка Linux/macOS отдельно не выполнялась.

Дополнительные команды:

```powershell
.\gradlew.bat test
.\gradlew.bat check
.\gradlew.bat genSources
```

Основной артефакт: `build/libs/nexvisuals-0.7.0-dev.jar` — production/remapped мод. Файл `-sources.jar` предназначен для изучения исходников, а не для установки. Нужны **Minecraft 1.21.11**, **Java 21+**, **Fabric Loader 0.19.5+** и **Fabric API 0.141.6+1.21.11** либо совместимый более новый API именно для 1.21.11. Дополнительных обязательных библиотек нет. Уберите старый NexVisuals JAR перед установкой нового.

Шесть предоставленных пользователем MP3 подготовлены в локальной ignored-папке `.tools/user-sounds`. Для сборки с этими клипами используйте `clean build -PuserSoundResources=.tools/user-sounds`; обычный `clean build` использует vanilla event fallbacks и не включает чужие аудиофайлы. Готовый JAR текущего этапа собран **с клипами**, отдельный resource pack не нужен. Происхождение файлов и подготовка описаны в [Sound Feedback](docs/sound-feedback.md) и [ASSETS](ASSETS.md).

## Что доступно

- Обновление 0.7: **шесть пользовательских Hit Sounds**, настраиваемое приглушение vanilla attack cues, отдельные **Totem Pop Sounds** и **Totem Tracker HUD**. Preview, presets, config/profiles и общий HUD Editor; счётчик отслеживает реальные локальные срабатывания. [Настройки и ручная проверка](docs/sound-feedback.md).
- Обновление 0.6: **Equipment HUD, Item Counter, Active Visuals, Status Effects HUD**, общая **HSV/RGB/alpha-палитра**, анимации существующего прицела, **Low HP / Damage Pulse** в Screen Edge Tint и **Hearts / Pixels** в Hit Effects. Позиции/scale новых HUD используют прежний редактор. Настройки/профили общие, новых Mixins нет. [Описание и checklist 0.6](docs/hud-customization.md).
- Набор 0.5 сохранён: **Jump / Landing Rings, Footstep Effects, Totem Echo, Block Interaction FX, Cosmetic Orbitals, Weather Lens, Underwater FX, Retro Display**. Первые пять — частицы в мире с нормальной глубиной; последние три — GPU-pattern/refraction/pixel effects перед HUD. Подробности: [обновление 0.5](docs/new-visuals.md).
- Custom Skybox: процедурный купол, звёзды, sun/moon, aurora/nebula/meteors; девять стилей. **Enhanced Day** и **Dynamic / NexVisuals** добавляют отдельную утреннюю палитру, плавный полный цикл суток, дневную дымку и мягкий солнечный ореол. Прежние ночные presets сохранены.
- Live Background: шесть процедурных GPU-обоев **NexVisuals / Aurora / Flow / Nebula / Waves / Minimal** для главного меню и редактора, опционально pause menu. Три цвета с alpha, скорость, движение, мягкость форм, яркость, насыщенность, intensity, motes и dim; режим Vanilla. Работает вместе с Console Menu.
- Lightweight Shaders: grading, exposure/highlights, vignette, chromatic aberration, grain, color/night filters и damage flash; восемь стилей. **Dreamy** и обновлённый **Cinematic** используют широкое выделение ярких участков и мягкую периферию. Compact glow остаётся; Wide добавляет маленький quarter-resolution pass. Активный Iris shader pack приостанавливает sky/post renderer.
- Console Menu: главное меню в духе классических консольных изданий, с родной панорамой, логотипом и шрифтом Minecraft. Classic/Sunset/Moonlight/Still, цвета, положение панели, скорость/направление движения и оформление загрузки мира.
- Hit Effects: Burst, Sparks, Rings, Slash, Impact, Hearts, Pixels; собственные процедурные masks, цвета/alpha, motion, scale/fade/easing и marker. Hit Sounds: шесть прежних vanilla voices и шесть предоставленных клипов, volume/pitch/variation и quieter vanilla attacks.
- Viewmodel: position/rotation, общий и per-axis scale, две руки, Copy/Mirror и шесть presets. Item Swing: семь стилей, duration/easing/amplitude и Custom transforms.
- Fire Overlay, отдельные resting/blocking Shield transforms, ограниченные по lifetime Player Trails и локальный Cosmetic Hat.
- Fireflies: дрейфующие мерцающие огоньки с цветами, плотностью, радиусом и режимом dusk/night. Elytra Trails: парные потоки только при локальном полёте, стили Aurora/Comet/Halo.
- Custom Crosshair со статичными и анимированными presets, одиннадцать собственных HUD элементов и общий framework для восьми vanilla HUD layers с HUD Editor, позициями, scale, backgrounds и outlines.
- Hotbar / Mini HUD: единый масштаб нижних индикаторов вокруг центра экрана, включая воздух, mount health и item name. Keystrokes показывает плитки с подсветкой фактически нажатых клавиш.
- Container Visuals: общие panel/slot/hover/click эффекты и quick-move ghosts по однозначным локальным slot changes.
- Sky Palette для vanilla Overworld sky; безопасная кнопка открытия публичного Iris settings screen при его наличии. Управление shader-pack presets не реализовано.
- Локальные профили: save/load/update/rename/delete/defaults; встроенные общие стили Clean, Aurora и Cinematic. GUI генерирует настройки и module presets из metadata.

Подробности, различия presets и сознательные ограничения — в [списке возможностей](docs/features.md). Новые функции **собраны и автоматически проверены, но визуально/runtime не проверены** в этом этапе. Запуск игры и visual QA выполняет пользователь.

## Использование

Откройте NexVisuals клавишей **Right Shift**, когда игрок находится в мире. Дополнительная кнопка есть в меню паузы. Привязка меняется через стандартные Minecraft Options → Controls → Key Binds → NexVisuals и хранится Minecraft в `options.txt`.

Для нового главного меню нажмите **«Меню NexVisuals»** в левом верхнем углу vanilla title screen. Кнопка **«Стиль меню...»** открывает настройки Console Menu, **Vanilla** возвращает обычное меню. Модуль также доступен через Interface → Console Menu. Изменения компоновки видны после возврата из настроек; цвета и движение сохраняются в общем config и профилях. HUD Editor доступен только при загруженном мире.

Для живых обоев: **Interface → Live Background → Enabled → Waves → Apply**. Пресет Waves вдохновлён спокойными изогнутыми полосами референса; другие presets меняют саму форму фона. Настройки видны сразу за редактором. Console Menu продолжает управлять панелью/кнопками, Live Background — только обоями. **Background → Vanilla** либо выключение Live Background возвращает прежний фон, сохраняя цвета. Кнопка Vanilla в Console Menu отключает его компоновку отдельно. Reduced motion из Console Menu останавливает и живой фон. Preview/F4 намеренно показывает мир, скрывая и редактор, и обои.

Все модули изначально выключены. Настройки применяются в памяти сразу, записываются при закрытии экрана и штатном завершении клиента. Цвета используют формат `#AARRGGBB`: первые две цифры задают непрозрачность. Кнопка **Picker** открывает HSV-поле, RGB/HSV-слайдеры и отдельный alpha; изменения Live Background видны за палитрой. Done/Esc применяет, Cancel возвращает цвет до открытия. Кнопка `R` сбрасывает одну настройку, `Reset settings` — настройки выбранного модуля. Числовыми слайдерами можно управлять клавишами. `Ctrl+F` переводит фокус в поиск; `Page Up`/`Page Down` прокручивают настройки.

Некорректный текст в редакторе выделяется красным: последнее корректное значение остаётся действующим. Встроенные module presets выбираются кнопкой со стрелкой и применяются через `Apply`; затем можно менять любые параметры. В Profiles кнопка `Apply style` применяет общий стиль Clean/Aurora/Cinematic, заменяя активную конфигурацию; перед этим можно сохранить свою. Для цветов есть быстрые swatches. В General настраивается opacity меню. Панель настроек не является финальным дизайном.

Для нового неба: **World → Custom Skybox → Dynamic / NexVisuals или Enhanced Day → Apply → Enabled**. Cyber / Purple Nebula и остальные старые стили остаются доступны. Декоративные ночные слои видны вечером/ночью; время мира не подменяется. В `Section → Dynamic cycle` можно редактировать morning palette, phase brightness, haze, sun halo и cycle fog. **Post Processing → Lightweight Shaders → Dreamy** демонстрирует новые эффекты. `Current` показывает совпадающий стиль или `Custom` после правки. **Preview / F4** скрывает редактор; **F4 / Esc** возвращает его. Ввод остаётся внутри экрана, без атак или изменения скрытых controls. Причина приостановки renderer отображается над пресетами и в tooltip.

HUD Editor открывается кнопкой в меню: drag для позиции, Shift для snap, стрелки для точной правки, right-click для enabled, `H` для скрытия нижней панели. Профили находятся в `config/nexvisuals/profiles/`, основной config — `config/nexvisuals.json`.

Для исправленного мини-HUD: **HUD → Hotbar / Mini HUD → Mini → Apply**, включить модуль. `Linked Mini HUD` включён по умолчанию и использует один масштаб/позицию для всей нижней группы. Отдельные настройки дочерних слоёв сохраняются; отключите Link для независимого редактирования. Сброс всего конфига не требуется.

Hit Effects и Hit Sounds показывают **локальную попытку атаки по видимой сущности**, а не подтверждение урона сервером. Все изменения остаются клиентскими; мод не изменяет reach, cooldown, hitboxes, item transfer logic или packets.

## Документация

- [Версии и первичные источники](docs/versions.md).
- [Модули, пресеты и ограничения](docs/features.md).
- [Архитектура и добавление модулей](docs/architecture.md).
- [Результаты проверки и ограничения](docs/validation.md).
- [Hit Sounds, Totem Pop Sounds, Totem Tracker и локальные клипы](docs/sound-feedback.md).
- [Восемь новых эффектов: исследование, настройки и ручная проверка](docs/new-visuals.md).

`src/main/java/dev/nexvisuals/core` содержит независимую Java-логику; `src/client/java/dev/nexvisuals/client` — интеграцию с игрой. Метаданные Fabric объявляют `environment: client` и точную зависимость `minecraft: =1.21.11`.

`build` запускает core tests, headless `clientTest`, проверку bytecode-контрактов Mixins и состава production JAR. Ни один тест не открывает окно Minecraft. Отдельно: `gradlew clientTest`, `gradlew verifyModJar`, `git diff --check`.

GLSL проверяется отдельно командой `tools/validate_shaders.ps1 -Validator /path/to/glslang.exe -MinecraftJar /path/to/1.21.11/minecraft-client.jar`: compile/link пяти программ с настоящими vanilla imports и размеры uniform blocks. Скрипт ничего не скачивает и не открывает игру. Процедура и ограничения проверки описаны в [validation](docs/validation.md).

Gradle cache, development runtime, логи, crash reports, IDE-файлы и сборочные артефакты исключены из Git. Wrapper JAR — официальный проверенный служебный бинарник; PNG masks — исходные ресурсы мода. Их происхождение описано в [ASSETS.md](ASSETS.md). Решение о лицензии основного кода и публикации остаётся владельцу проекта. Codex не выполняет commit/push/tag/release.

Это не официальный продукт Minecraft; он не связан с Mojang или Microsoft.
