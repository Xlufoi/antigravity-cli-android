# Antigravity Mobile (Android)

Нативное мобильное приложение для Google Antigravity на базе **Kotlin** и **Jetpack Compose** (Material 3).

## Архитектура
* **UI**: Jetpack Compose, Material 3, Dark/Neon Theme.
* **IPC**: Прямое взаимодействие со встроенным нативным ядром (`libagy.so` / ARM64) через NDJSON (`stream-json`) по стандартным потокам `stdin`/`stdout`.
* **Фоновый режим**: `ForegroundService` с постоянным уведомлением, защищающим процесс от остановки системой Android.

## Сборка
Проект автоматически собирается в APK с помощью GitHub Actions.
После завершения workflow APK доступен для скачивания во вкладке **Actions -> Artifacts**.
