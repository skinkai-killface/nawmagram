# Спецификация плагинов Nawmagram (.naw)

Формат `.naw` — это ZIP-пакет для Nawmagram (форка Telegram Android), содержащий:
1. `classes.dex` — скомпилированный байткод классов плагина.
2. `plugin.json` — манифест плагина.
3. `assets/` — опциональные ресурсы (иконки, темы, звуки).

## Структура `plugin.json`:
```json
{
  "id": "org.nawmagram.sample",
  "name": "Sample Plugin",
  "version": "1.1.0",
  "author": "Author",
  "description": "Описание плагина",
  "main": "org.nawmagram.sample.MainPlugin"
}
```

## Интерфейс `NawmaPlugin`:
* `onModifyThemeColor(int colorKey, int defaultColor)` — подмена цвета тем.
* `onActionBarCreated(ActionBar actionBar, BaseFragment fragment)` — модификация шапки экранов.
* `onCommand(String command, String[] args, long dialogId, BaseFragment fragment)` — перехват команд в чате (.ping, .help).
* `onMessageSend(long dialogId, String text)` — перехват/модификация отправляемого сообщения.
