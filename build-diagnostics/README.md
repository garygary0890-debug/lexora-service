# Lexora Service — build diagnostics

Этот каталог предназначен для автоматической фиксации проблем сборки Android-проекта.

## Основные файлы
- `latest-build.log` — полный вывод последней диагностической сборки.
- `latest-summary.txt` — краткий статус последней диагностической сборки.
- `git-status.txt` — состояние Git на момент диагностической сборки.
- `working-tree.patch` — снимок незакоммиченных изменений tracked-файлов, исключая `build-diagnostics/**`.
- `history/*.log` — исторические логи отдельных запусков.

## Запуск из Android Studio
В проект добавлена общая Run Configuration **Build with diagnostics**.
Она запускает `scripts/build-with-diagnostics.ps1`, который:
1. проверяет наличие полноценного Gradle Wrapper;
2. если `gradlew.bat` или `gradle-wrapper.jar` отсутствуют — запускает `scripts/bootstrap-gradle-wrapper.ps1` и генерирует Wrapper для Gradle 9.4.1;
3. запускает `gradlew.bat assembleDebug --stacktrace --warning-mode all`;
4. записывает вывод в `build-diagnostics/latest-build.log`;
5. сохраняет копию в `build-diagnostics/history/`;
6. пишет итог в `latest-summary.txt`;
7. сохраняет `git-status.txt` и `working-tree.patch`;
8. коммитит диагностические файлы;
9. выполняет `git push` в текущую ветку, если Git и доступ к GitHub настроены.

## Gradle Wrapper bootstrap
`gradle/wrapper/gradle-wrapper.properties` закрепляет Gradle 9.4.1.
Если стандартные wrapper-файлы отсутствуют, `scripts/bootstrap-gradle-wrapper.ps1`:
- загружает официальный `gradle-9.4.1-bin.zip` с `services.gradle.org` в `%USERPROFILE%\.lexora\gradle-bootstrap`;
- распаковывает Gradle во временный пользовательский кэш;
- выполняет Gradle task `wrapper`;
- проверяет наличие `gradlew`, `gradlew.bat`, `gradle-wrapper.jar` и `gradle-wrapper.properties`;
- при доступном Git коммитит и отправляет сгенерированные wrapper-файлы в текущую ветку.

## Важно
- Если bootstrap Wrapper не выполнен, диагностическая сборка получает статус `BOOTSTRAP_FAILED`, а причина сохраняется в логах.
- Исходный код приложения автоматически в диагностический коммит не добавляется.
- `working-tree.patch` позволяет видеть незакоммиченные изменения tracked-файлов, которые могли вызвать ошибку сборки.
- Содержимое новых untracked-файлов автоматически в patch не включается; их имена перечисляются в patch.
- Если push не удался, лог остается локально и скрипт выводит предупреждение.
- Для локального запуска без push используйте:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-with-diagnostics.ps1 -NoPush`
- Для другой Gradle-задачи используйте, например:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-with-diagnostics.ps1 -Task testDebugUnitTest`

После появления `latest-build.log`, `latest-summary.txt`, `git-status.txt` и `working-tree.patch` в GitHub ChatGPT может читать их из репозитория и использовать при исправлении ошибок сборки.
