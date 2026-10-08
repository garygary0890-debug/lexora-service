# Planning Date Controls Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the duplicate Planning heading and place a full-width Today button between previous and next chevrons.

**Architecture:** Keep the screen title in the app top bar and preserve day/week/month navigation semantics while replacing the round previous/next controls.

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

### Task 1: Planning heading and date control row

**Files:**
- Modify: `feature/planning/src/main/java/com/lexora/service/feature/planning/PlanningScreen.kt`
- Modify: `feature/planning/src/main/res/values/strings.xml` if accessibility labels for chevrons are missing
- Test: `feature/planning/src/androidTest/java/com/lexora/service/feature/planning/PlanningDateControlsTest.kt`

**Interfaces:**
- Consumes: existing `onPrevious`, `onToday`, and `onNext` callbacks.
- Produces: a row with previous chevron, weighted Today button, next chevron; only the top bar shows the Planning title.

- [ ] Add UI tests for heading count/location, Today button width, icon descriptions, and callback behavior for all three controls.
- [ ] Run the focused UI test and verify it fails for the duplicate heading and current text buttons.
- [ ] Remove the content title; replace previous/next controls with accessible chevron IconButtons around a full-width-weighted Today button.
- [ ] Run focused UI tests for day/week/month modes and verify all callbacks work.
- [ ] Commit this task.

### Task 2: Regression verification

**Files:**
- Modify: `docs/LEXORA_SERVICE_CHANGELOG.md`

- [ ] Run `./gradlew :feature:planning:test :feature:planning:connectedAndroidTest :app:assembleDebug` on the configured Android runner.
- [ ] Verify date-anchor display and day/week/month chips are unchanged.
- [ ] Record results and commit the changelog update.
