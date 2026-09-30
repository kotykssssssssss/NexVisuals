# HUD и визуальная кастомизация 0.6

Этот этап расширяет текущий NexVisuals для **Minecraft 1.21.11**. Добавлены четыре HUD modules и четыре расширения существующих систем. Повторы исключены по каталогу NexVisuals; отдельный NexLauncher не изменялся. Встроенные shader effects, night Skybox, Console Menu, Live Background, mini HUD, Fire/Shield и предыдущие частицы сохранены.

## Новые HUD elements

Открыть **Right Shift → HUD** в мире. Каждый модуль включается отдельно. **HUD Editor** позволяет двигать его мышью, изменять scale и сохранять относительные позиции. Anchor/offset остаются доступны как раньше. Общие настройки: text/shadow, panel color/alpha, rounding, scale .5–3. Все выключены по defaults.

| Модуль | Данные и настройки | Presets |
| --- | --- | --- |
| Equipment HUD | Четыре armor slots, optional main/off hand; горизонтально/вертикально; hide empty; durability bars/percent; warning threshold/color | Equipment Strip, Armor Cards, Minimal Gear |
| Item Counter | Сумма в своих 36 inventory slots + off hand; Held/Totems/Pearls/Arrows/Rockets; icon/name toggles | Totems, Pearls, Held Item |
| Active Visuals | Enabled NexVisuals modules; alphabetical/width sort; optional HUD/preset names; accent line/soft halo; max 3–24 rows плюс bounded строка остатка | Clean List, Detailed, Minimal |
| Status Effects HUD | Иконки из resource pack, названия, level, remaining duration/∞; own effects with vanilla showIcon flag; optional effect-colored text; max 1–12, hide when empty | Effect Cards, Colored Effects, Minimal Timers |

Active Visuals изначально справа сверху; остальные используют левую сторону, дальше положение выбирается в Editor. Ни один элемент не читает чужой inventory/HP. Item Counter считает **тип предмета**, не components: обычные arrows отдельно от tipped/spectral; contents shulker/containers не просматриваются. Status HUD дополняет существующий Vanilla HUD → Effects, не заменяет vanilla indicators. Уровни выводятся числом. При отсутствии эффектов выключаемый пустой panel остаётся selectable в editor.

## Общая Color Picker

У всех ColorSettings появилась кнопка **Picker**. Слева drag-and-drop saturation/value field и hue/alpha strips с checkerboard; справа переключаемые HSV/RGB sliders, alpha и точный `#AARRGGBB` hex. Hue не теряется при переходе к black/gray. Некорректный hex красный и не меняет последнее корректное значение.

- **Done / Esc:** сохранить выбранный цвет в памяти и вернуться к настройкам; обычное сохранение config происходит при закрытии редактора/выходе.
- **Cancel:** восстановить ARGB до открытия палитры.
- **Reset:** default текущей ColorSetting.
- **Tab/arrow keys:** стандартный focus; стрелки меняют pad/channel, Shift увеличивает шаг.
- Live Background остаётся видимым за палитрой; цвета обоев изменяются сразу. Старые hex/swatches работают как раньше.

Поле рисуется 32 cached цветными strips через GUI primitives; textures/framebuffers не создаются. Это общий native Screen для типа настройки, а не отдельный UI framework или экран под конкретный модуль.

## Прицел

В прежнем **Custom Crosshair** добавлены Animation OFF/BREATHE/MOVEMENT/SWING/ROTATE, animation amount, breathing speed и rotation speed. Дыхание и вращение используют game time + partial tick; движение и swing берутся только от своего игрока. Анимация не означает weapon spread/accuracy/cooldown/подтверждённый hit.

Новые presets: **Breathing Orbit, Motion Cross, Swing Chevron, Rotating Cross**. Старые Classic/Dot/Wide/Precision/Orbit/Chevron остаются статичными. Outline/dot/цвета/reset сохранены. Анимируется custom reticle; vanilla attack indicator и его расчёт остаются вне pose transform. Максимальный дополнительный scale ограничен 1, итоговый scale — 1–2. Geometry spans кэшируются как раньше.

## Low HP / Damage Pulse

В прежнем **World → Screen Edge Tint**: Static, Low Health, Damage, Breathe. Настройки intensity, local health threshold и pulse speed. Presets **Soft Edges, Low HP Pulse, Damage Pulse, Calm Breath**.

Low HP Pulse постепенно усиливается ниже выбранной доли своего max health и делает два коротких heartbeat peaks. Damage Pulse использует уже существующий локальный vanilla hurt timer. Vanilla damage/fire overlays не отменяются. У старых конфигов прежний Static и прежние color/size; дополнительного дублирующего screen module нет.

## Hit Effects: Hearts и Pixels

- **Hearts:** оригинальная heart mask, более крупные upright сердечки с подъёмом/дрейфом; розовый gradient, longer lifetime, меньше частиц. Preset отключает random rotation, но настройку можно менять.
- **Pixels:** crisp square mask, быстрый радиальный разлёт с вращением/падением и shorter lifetime. Все прежние controls color/alpha/amount/scale/easing/fade/reset доступны.

Оба используют существующий textured particle pipeline с depth testing. Budget emission **128 за tick**, active cosmetic particles **768**, максимум detail per attempt **96** сохранены. Пресеты не создают history или custom renderer. Это feedback локальной попытки атаки по доступной сущности, а не подтверждённый сервером damage. Нет damage numbers/target HP inference.

## Проверка вручную

1. Новый production JAR поставить вместо старого в Fabric **1.21.11**; проверить `latest.log` на NexVisuals/mixin errors.
2. Equipment HUD: armor/две руки, damaged/empty items, bars/percent/threshold, оба layouts и три presets; двигать/масштабировать через Editor, изменить GUI scale/окно.
3. Item Counter: несколько stacks + off hand, held item/empty hand, выбрать pearls/totems/arrows/rockets; проверить icon/name и цифры после перемещения своих предметов.
4. Active Visuals: toggle несколько модулей, include HUD, names/presets/Custom, sort, max rows и overflow label; расположение справа/после resize.
5. Status Effects HUD: известный local potion effect, несколько effects/levels, timers/∞, native icons, effect colors; пропадание после окончания и selectable editor panel без effects.
6. Color Picker: mouse drag/Tab/arrows/Shift, HSV/RGB/alpha/hex, invalid hex, black/gray hue retention, Cancel/Reset/Done/Esc. Цвета Live Background должны меняться за палитрой; menu buttons/input/layout остаются прежними.
7. Crosshair: четыре новых presets; стоять/двигаться/махать рукой, circle/dot/outline/rotation; vanilla attack indicator, F3 3D crosshair, scope/spectator fallback, OFF возвращает static.
8. Screen Edge Tint: Low HP threshold и heartbeat только своего HP, Damage Pulse при hurt, Breathe; Static сохраняет старый вид. Сравнить intensity 0/1 и module OFF.
9. Hit Effects: Hearts/Pixels, orientation/spin/gravity/size/fade/colors, OFF; другие Burst/Sparks/Rings/Slash/Impact сохранены. Проверить depth/particle limits при частых атаках.
10. Config: сохранить profile, изменить/восстановить/rename, перезапустить игру, проверить enabled/colors/positions/animations. Кратко проверить старый night Skybox, mini HUD, Fire/Shield, Console Menu/Live Background.
11. FPS/resource reload F3+T; Sodium отдельно, Iris с/без shader pack. Совместимость и GPU output этой сборки автоматически не подтверждены.

Automated результаты и фактический JAR — в [validation](validation.md). Minecraft/computer-use не запускались. Полноценный shader pack, glow скрытых entities/blocks, inventory automation, fake damage/kill statistics и дополнительные аналоги уже имеющихся модулей не добавлены.
