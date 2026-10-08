# Configure Bottom Navigation from Menu Overflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move Configure bottom navigation into the three-dot overflow menu on the Menu screen.

**Architecture:** Use the existing MoreVert top-bar menu and preserve the current preference storage, destination validation, and ordering behavior.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Android Gradle Plugin, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-08-service-user-workspaces-themes-design.md`

## Global Constraints

- Все изменения выполняются только в репозитории `garygary0890-debug/lexora-service`.
- Отдельные проекты и серверный репозиторий не изменяются.
- Доступ к данным пользовательской рабочей области получают её владелец и явно подключённые участники команды.
- Миграция Room не удаляет и не объединяет данные молча; неоднозначное сопоставление требует разрешения.
- Темы строятся на семантических цветовых токенах; геометрия компонентов между темами не меняется.

## Review Focus

1. Старый пользователь состоит в нескольких организациях — тест миграции подтверждает сохранение всех рабочих доступов пользователя.
2. Участник команды без членства обращается к чужим данным — тест репозитория подтверждает отказ.
3. Конфликт номеров заявок после преобразования области владения — тест миграции подтверждает отсутствие потери ссылок.
4. Обновление схемы прерывается при неоднозначном владельце — тест подтверждает сохранность исходной базы.
5. Сервер продолжает требовать старый контекст организации — проверка клиента фиксирует несовместимость и не подменяет ответ фиктивным успехом.

---

### Task 1: Relocate the configuration action

**Files:**
- Modify: `app/src/main/java/com/lexora/service/AppNavigationChrome.kt`
- Modify: `app/src/main/java/com/lexora/service/MainActivity.kt`
- Modify: `app/src/main/java/com/lexora/service/BottomNavigationPreferences.kt` only if user-scoped identity changes in Task 1 of the workspace plan
- Test: `app/src/androidTest/java/com/lexora/service/MenuOverflowNavigationTest.kt`

**Interfaces:**
- Consumes: existing bottom destinations and preference read/write flow.
- Produces: a «Настроить нижнее меню» overflow item shown on Menu only; the existing configure UI opens from that item.

- [ ] Add Compose UI tests asserting the item appears in the Menu top-bar overflow, is absent from the Menu body, and opens the editor.
- [ ] Run the UI test and verify it fails while the action is only in the screen body.
- [ ] Move the action into the existing MoreVert dropdown and preserve save, ordering, and the three-item limit.
- [ ] Run the focused UI test and route tests; verify saved order remains after navigation.
- [ ] Commit this task.

### Task 2: Regression verification

**Files:**
- Modify: `docs/LEXORA_SERVICE_CHANGELOG.md`

- [ ] Run `./gradlew :app:test :app:connectedAndroidTest :app:assembleDebug` on the configured Android runner.
- [ ] Verify other top-bar actions remain available on screens where they belong.
- [ ] Record results and commit the changelog update.
