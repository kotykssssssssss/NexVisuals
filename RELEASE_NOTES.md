# NexVisuals 1.0.0 — Minecraft 1.21.11 / Fabric

NexVisuals — клиентский мод визуальной кастомизации Minecraft: собственное небо, эффекты ударов, предметы от первого лица, cosmetics, HUD и меню. Все изменения косметические; мод не добавляет боевую или инвентарную автоматизацию.

## В этом релизе

- **Projectile Trails:** три стиля — Comet, Embers и Pearl Halos. Настраиваются цвета/alpha, размер, плотность, lifetime, дальность и типы снарядов. Только наблюдаемые позиции стрел, трезубцев, жемчуга и других брошенных предметов; стены прерывают emission, будущая траектория не предсказывается.
- **Pickup HUD:** временные карточки собственных подтверждённых подборов с настоящими item icons, количеством и названиями. Повторные подборы одного item+components объединяются. Stacked / Compact / Minimal, Slide / Pop / Fade, duration, число строк, цвета и общий HUD Editor с drag/scale/anchors.
- **Предрелизные исправления:** стабильные описания и прокрутка в поиске; исправленные Viewmodel PvP/Cinematic; одиночные Item Swing animations; выравнивание Weapon Trails и first-frame Player Trails crash fix сохранены.
- **Оформление сборки:** версия 1.0.0, MIT metadata/notice внутри JAR, описание ассетов и явный индикатор audio build mode в Hit Sounds / Totem Pop Sounds.

## Основные возможности NexVisuals

- Custom Skybox: плавный Dynamic day/night cycle, звёзды, солнце/луна, gradient, nebula, aurora и horizon effects.
- Lightweight post-processing: color grading, selective glow, vignette, grain, chromatic, cinematic и Retro/Underwater/Weather effects. Это не полноценный сторонний shader pack.
- Hit Effects с разными формами/поведением, Hit Sounds, Totem Pop Sounds / Animation / Echo.
- Viewmodel: две руки, position/rotation/per-axis scale, presets и Item Swing; отдельные Shield / Fire Overlay настройки.
- Player / Elytra / Weapon / Projectile Trails, Cosmetic Hat, Fireflies, Orbitals, Jump / Landing Rings и Footstep / Block effects.
- Crosshair, Custom / Vanilla HUD, HUD Editor, Container Visuals, локальные profiles и presets.
- Console Menu и процедурные Live Background с настраиваемыми цветами и движением.

## Установка

Нужны **Minecraft Java Edition 1.21.11**, **Java 21+**, **Fabric Loader 0.19.5+**, **Fabric API 0.141.6+1.21.11**. Sodium и Iris не обязательны; runtime compatibility с ними требует самостоятельной проверки. Уберите предыдущий NexVisuals JAR из `mods`, затем установите ровно один новый production JAR. Sources JAR не устанавливать.

Все модули выключены по умолчанию. Меню открывается **Right Shift в мире** или кнопкой NexVisuals в pause menu. Примените preset через `Apply` и включите модуль. Основной config — `config/nexvisuals.json`, profiles — `config/nexvisuals/profiles/`; старые configs сохраняются, удалять их не требуется.

## Два варианта аудио

- **Для GitHub:** `build/release/nexvisuals-1.0.0.jar`. Без присланных MP3/OGG. Звуковые функции и пункты меню остаются: соответствующие sound IDs ссылаются на события установленного Minecraft.
- **Личный вариант:** `build/libs/nexvisuals-1.0.0.jar`. Содержит все шесть предоставленных клипов для этой пользовательской установки. Источник — Myinstants.com со слов пользователя; права на публичное распространение конкретных файлов не установлены. Этот JAR не является публичным release asset.

Не устанавливайте оба варианта одновременно. Исходные clips не добавлены в репозиторий; подробности происхождения и прав — в [ASSETS.md](ASSETS.md).

## Проверка и ограничения

Проект проверяется clean Gradle build, полным core/headless client suite, bytecode-контрактами Mixins, GLSL compile/link и содержимым production JAR. Актуальные результаты — в [docs/validation.md](docs/validation.md).

Minecraft при подготовке этой сборки не запускался. Новые визуальные эффекты, FPS и Sodium/Iris пользователь проверяет в игре. Pickup icons используют штатный item renderer; alpha fade применяется к подписям/акцентам, Pop — к строке. Неизвестные pickup entities/owners не угадываются. При Minimal particles Projectile Trails не создаёт частиц. Полный Iris shader pack management, motion blur, ray tracing и cheats не входят в релиз.
