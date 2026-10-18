# 📱 Nawmagram

<div align="center">

![Nawmagram](https://img.shields.io/badge/Nawmagram-v13.0.2--X-blueviolet?style=for-the-badge&logo=telegram)
![Build](https://img.shields.io/github/actions/workflow/status/nawmbad/nawmagram/build_apk.yml?style=for-the-badge&label=APK%20Build)
![License](https://img.shields.io/badge/License-GPL--2.0-orange?style=for-the-badge)

**Кастомный форк Telegram Android — Deep Space тема, градиентные пузырьки, Material You UI**

</div>

---

## ✨ Отличия от официального Telegram

| Фича | Nawmagram | Telegram |
|------|-----------|---------|
| 🎨 Deep Space тема | ✅ по умолчанию | ❌ |
| 💬 Градиентные пузырьки | ✅ | ❌ |
| 🌈 3 тёмных темы | ✅ | частично |
| ⚡ ThemeManager | ✅ | ❌ |
| 📦 Package ID | `org.telegram.nawmagram` | `org.telegram.messenger` |

## 🎨 Палитра Deep Space

| Элемент | Цвет | Preview |
|---------|------|---------|
| Фон | `#0A0E1A` | 🟦 |
| Поверхность | `#111827` | 🔷 |
| Акцент | `#6C63FF` | 🟣 |
| Розовый | `#FF6B9D` | 🩷 |
| Онлайн | `#06D6A0` | 🟢 |

## 📦 Скачать APK

Готовые APK доступны во вкладке **[Actions](../../actions)** → последний workflow → **Artifacts**.

## 🛠 Как работает сборка

Этот репозиторий содержит **только наши изменения** (overlay).  
GitHub Actions автоматически:
1. Клонирует официальный [Telegram Android](https://github.com/DrKLO/Telegram)
2. Применяет наши патчи (тема, имя, пакет)
3. Собирает APK и выкладывает как артефакт

## ⚙️ Настройка сборки (обязательно!)

Добавь секреты в репозиторий:

**Settings → Secrets and variables → Actions → New repository secret**

| Secret | Описание |
|--------|---------|
| `TELEGRAM_APP_ID` | App ID с [my.telegram.org](https://my.telegram.org) |
| `TELEGRAM_APP_HASH` | App Hash с [my.telegram.org](https://my.telegram.org) |

## 📁 Структура

```
nawmagram/
├── .github/workflows/
│   └── build_apk.yml        # CI: клонирует TG + патчит + собирает APK
└── overlay/
    ├── gradle.properties     # APP_PACKAGE=org.telegram.nawmagram
    ├── res/
    │   ├── values/colors.xml         # Deep Space палитра
    │   ├── values-night/colors.xml   # Ночная версия
    │   ├── values-night/styles.xml   # Material You стили
    │   └── values-v21/styles.xml     # API 21+ стили
    └── java/.../fork/theme/
        └── ThemeManager.kt   # Движок тем (3 dark-темы)
```

## 🔗 На основе

[Telegram for Android](https://github.com/DrKLO/Telegram) © Telegram, GPL-2.0

## 📄 Лицензия

GPL-2.0
