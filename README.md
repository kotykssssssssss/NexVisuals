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

Основной артефакт: `build/libs/nexvisuals-0.1.0-dev.jar`. Файл `-sources.jar` предназначен для изучения исходников, а не для установки в Minecraft. Для обычной установки нужны Minecraft **1.21.11**, Fabric Loader и Fabric API соответствующих версий.

## Использование

Откройте NexVisuals клавишей **Right Shift** в мире или кнопкой **NexVisuals** в главном меню/меню паузы. Привязка меняется через стандартные Minecraft Options → Controls → Key Binds → NexVisuals и хранится Minecraft в `options.txt`.

Все модули изначально выключены. Настройки применяются в памяти сразу, записываются при закрытии экрана и штатном завершении клиента. Цвета используют формат `#AARRGGBB`: первые две цифры задают непрозрачность. Кнопка `R` сбрасывает одну настройку, `Reset settings` — настройки выбранного модуля. Числовыми слайдерами можно управлять клавишами. `Ctrl+F` переводит фокус в поиск; `Page Up`/`Page Down` прокручивают настройки.

Некорректный текст в редакторе выделяется красным: последнее корректное значение остаётся действующим. Панель настроек не является финальным дизайном; её структура подготовлена для последующего редизайна по визуальным референсам.

## Документация

- [Версии и первичные источники](docs/versions.md).
- [Архитектура и добавление модулей](docs/architecture.md).
- [Результаты проверки и ограничения](docs/validation.md).

`src/main/java/dev/nexvisuals/core` содержит независимую Java-логику; `src/client/java/dev/nexvisuals/client` — интеграцию с игрой. Метаданные Fabric объявляют `environment: client` и точную зависимость `minecraft: =1.21.11`.

Gradle cache, development runtime, логи, crash reports, IDE-файлы и сборочные артефакты исключены из Git. Единственный служебный бинарный файл репозитория — официальный проверенный Gradle Wrapper JAR. Репозиторий создаётся без commit/push; решение о лицензии и публикации остаётся владельцу проекта.

Это не официальный продукт Minecraft; он не связан с Mojang или Microsoft.
