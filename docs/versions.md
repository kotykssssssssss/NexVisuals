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

Проверено 28 сентября 2026 года. Источники:

- [Релиз Minecraft 1.21.11](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11).
- [Манифест Mojang](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json), [метаданные 1.21.11](https://piston-meta.mojang.com/v1/packages/bc03bf4398acc192063d758aecb1cb299f05d793/1.21.11.json).
- [Fabric для Minecraft 1.21.11](https://fabricmc.net/2025/12/05/12111.html).
- [Loader для 1.21.11, Fabric Meta](https://meta.fabricmc.net/v2/versions/loader/1.21.11).
- [Fabric API Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml), [Loom Maven metadata](https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml).
- [Документация HUD API 1.21.11](https://docs.fabricmc.net/1.21.11/develop/rendering/hud), [GUI 1.21.11](https://docs.fabricmc.net/1.21.11/develop/rendering/gui/custom-screens), [key mappings 1.21.11](https://docs.fabricmc.net/1.21.11/develop/key-mappings).

В исходном окружении PATH указывал на IBM Semeru JDK 17.0.17, а системный Gradle — на 9.4.0. Они не использовались для сборки. Найденный JDK 21: Oracle `21.0.9+7-LTS-338`; Gradle запускается через Wrapper. Системные установки и другие проекты не изменяются.

Wrapper JAR получен из официального репозитория Gradle, проверен по опубликованному SHA-256: `423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9`. Это необходимое исключение из правила не хранить сгенерированные бинарники: Wrapper позволяет собирать проект без установленного Gradle.

Смена Minecraft требует отдельной осознанной задачи: обновления метаданных, API, сборки и реального запуска. Совпадение названий методов с другой версией не подтверждает совместимость.
