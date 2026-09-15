# Lexora Service — build diagnostics

Этот каталог предназначен для автоматической фиксации проблем сборки Android-проекта.

## Основные файлы
- `latest-build.log` — полный вывод последней диагностической сборки.
- `latest-summary.txt` — краткий статус последней диагностической сборки.
- `history/*.log` — исторические логи отдельных запусков.

## Запуск из Android Studio
В проект добавлена общая Run Configuration **Build with diagnostics**.
Она запускает `scripts/build-with-diagnostics.ps1`, который:
1. запускает `gradlew.bat assembleDebug --stacktrace --warning-mode all`;
2. записывает вывод в `build-diagnostics/latest-build.log`;
3. сохраняет копию в `build-diagnostics/history/`;
4. пишет итог в `latest-summary.txt`;
5. коммитит только файлы диагностики;
6. выполняет `git push` в текущую ветку, если Git и доступ к GitHub настроены.

## Важно
- Если `gradlew.bat` отсутствует, это также фиксируется как `NOT_STARTED` и может быть отправлено в GitHub.
- Исходный код приложения автоматически в диагностический коммит не добавляется.
- Если push не удался, лог остается локально и скрипт выводит предупреждение.
- Для локального запуска без push используйте:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-with-diagnostics.ps1 -NoPush`
- Для другой Gradle-задачи используйте, например:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-with-diagnostics.ps1 -Task testDebugUnitTest`

После появления `latest-build.log` в GitHub ChatGPT может читать его из репозитория и использовать при исправлении ошибок сборки.
