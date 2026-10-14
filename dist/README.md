# dist

`FeatherW8-build.zip` — готовый снимок проекта для сборки APK на другой машине
(исходники + Gradle Wrapper, без `build/`, `.git`, `.gradle` и `local.properties`).

Внутри архива:

- проект раскрывается в папку `FeatherW8/`;
- `BUILD_APK_RU.md` — краткая инструкция по сборке на русском;
- `local.properties.example` — шаблон пути к Android SDK.

Содержимое соответствует коммиту, в который добавлен этот архив
(ветка `arena/d8055bf0-featherw8`). Сборка требует JDK 21, Android SDK 37,
NDK `27.2.12479018`, CMake `3.22.1` и интернет на первом запуске Gradle.
