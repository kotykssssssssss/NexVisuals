# Версии и источник решений

Цель этого репозитория — **Minecraft Java Edition 1.21.11**. Версия 1.21.1 не является заменой и не поддерживается этим артефактом.

| Компонент | Зафиксированная версия | Причина |
| --- | --- | --- |
| Minecraft | `1.21.11` | Явная цель проекта; официальный release в манифесте Mojang |
| Java | `21` | `javaVersion.majorVersion` в метаданных Mojang для 1.21.11 |
| Fabric Loader | `0.19.5` | Стабильная версия из Fabric Meta для 1.21.11 |
| Fabric API | `0.141.6+1.21.11` | Опубликованный артефакт именно для 1.21.11 |
| Fabric Loom | `1.14.10` | Стабильный выпуск рекомендованной для 1.21.11 ветки 1.14 |
| Mappings | Official Mojang mappings `1.21.11` | `loom.officialMojangMappings()`, привязаны к зависимости Minecraft |
| Gradle Wrapper | `9.2.1` | Зафиксированный Gradle для Loom 1.14, SHA-256 дистрибутива в wrapper properties |
| JUnit Jupiter | `5.11.4` | Тесты независимого Java-ядра |

Текущая версия NexVisuals — `0.7.0-dev`: шесть локально предоставленных звуков, приглушение vanilla attack cues, Totem Pop Sounds и Totem Tracker HUD поверх `0.6.0-dev`. Dynamic/Enhanced Day sky, ночные presets, GPU Live Background и все предыдущие модули сохранены. Стек Minecraft/Fabric не менялся. Новый `SoundEngineMixin` умножает только gain подходящих attack instances; существующий `ClientPacketListenerMixin` получил узкую правку local totem volume/pitch. Sites/дескрипторы проверяются по bytecode **1.21.11**. Runtime/audio-проверки оставлены пользователю по его прямому указанию. Описание этапа — в [sound-feedback](sound-feedback.md), предыдущего — в [hud-customization](hud-customization.md).

Проверено 28 сентября 2026 года. Источники:

- [Релиз Minecraft 1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11).
- [Манифест Mojang](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), [метаданные 1.21.11](https://piston-meta.mojang.com/v1/packages/bc03bf4398acc192063d758aecb1cb299f05d793/1.21.11.json).
- [Fabric для Minecraft 1.21.11](https://fabricmc.net/2025/12/05/12111.html).
- [Loader для 1.21.11, Fabric Meta](https://meta.fabricmc.net/v2/versions/loader/1.21.11).
- [Fabric API Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml), [Loom Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml).
- [Документация HUD API 1.21.11](https://docs.fabricmc.net/1.21.11/develop/rendering/hud), [GUI 1.21.11](https://docs.fabricmc.net/1.21.11/develop/rendering/gui/custom-screens), [key mappings 1.21.11](https://docs.fabricmc.net/1.21.11/develop/key-mappings).

Для обновления 30 сентября 2026 изучены [Fabric world rendering 1.21.11](https://docs.fabricmc.net/1.21.11/develop/rendering/world) и [Iris public API, ветка 1.21.11](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java). Fabric world events не предоставляют нужной узкой замены sky pass; используется точечный adapter `SkyRendererMixin`. Собственные shaders следуют UBO/RenderPipeline архитектуре Minecraft 1.21.11. Никаких private Iris shader option APIs или дополнительных обязательных mod dependencies нет.

Также просмотрены [Iris SkyRenderer hooks](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/main/java/net/irisshaders/iris/mixin/MixinSkyRenderer.java) и [GameRenderer hooks](https://github.com/IrisShaders/Iris/blob/1.21.11/common/src/main/java/net/irisshaders/iris/mixin/MixinGameRenderer.java) этой ветки. По этим исходникам явного удаления выбранных vanilla injection sites не обнаружено; это вывод из source review, не запуск совместной Mixin transformation и не подтверждение runtime конкретного Iris JAR.

Для проверки GLSL использован официальный [Khronos glslang 16.6.0](https://github.com/KhronosGroup/glslang/releases/tag/16.6.0), Windows x86_64 release ZIP. Его SHA-256 проверен по digest GitHub release asset: `82bf434e69b9bb4829de7e2b4bc2c5e7a7861e53d66cf75e5cc70f5f694a8d9b`. Команду проверки смотрите в validation; glslang не входит в JAR/зависимости проекта.

В исходном окружении PATH указывал на IBM Semeru JDK 17.0.17, а системный Gradle — на 9.4.0. Они не использовались для сборки. Найденный JDK 21: Oracle `21.0.9+7-LTS-338`; Gradle запускается через Wrapper. Системные установки и другие проекты не изменяются.

Wrapper JAR получен из официального репозитория Gradle, проверен по опубликованному SHA-256: `423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9`. Это необходимое исключение из правила не хранить сгенерированные бинарники: Wrapper позволяет собирать проект без установленного Gradle.

Смена Minecraft требует отдельной осознанной задачи: обновления метаданных, API, сборки и реального запуска. Совпадение названий методов с другой версией не подтверждает совместимость.
