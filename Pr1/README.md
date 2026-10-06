# Практична робота №1 — Варіант 10: «Система накопичення енергії (Battery Storage)»

Дисципліна: «Розробка програмного забезпечення мобільних пристроїв», КПІ ім. Ігоря Сікорського.

Тема: ознайомлення з Android Studio, Kotlin, Jetpack Compose. Створення першого «Hello World» застосунку.

## Вміст

- `BatteryStorageApp/` — Android-проєкт (Kotlin, Jetpack Compose, Material 3)
- `Звіт_ПР1_Варіант10.docx` — звіт
- `screenshots/` — скриншоти застосунку для звіту

## Режими застосунку

| Режим | Що реалізовано |
|---|---|
| Домашнє завдання | заголовок з 🔋, рівень заряду, потужність і режим, кнопка оновлення, Material Theme, Column/Row |
| Базовий рівень | текст «Hello Battery Storage», кнопка оновлення, колір фону |
| Середній рівень | SoC, потужність, енергія, температура; випадкове оновлення; Column + Row |
| Просунутий рівень | `animateIntAsState`, `animateFloatAsState`, `animateColorAsState`, індикатор батареї на Canvas, MaterialTheme |

## Запуск

Відкрийте теку `BatteryStorageApp` в Android Studio, дочекайтеся Gradle Sync і запустіть конфігурацію `app` на емуляторі (API 26+).
