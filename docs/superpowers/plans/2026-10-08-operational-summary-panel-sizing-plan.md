# Operational Summary Panel Sizing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make all six metric panels equal in size while keeping Expected Payments as a separate wide panel.

**Architecture:** Keep the current two-row KPI grid and apply a shared equal-height panel layout so labels and values do not change sibling dimensions.

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

### Task 1: Equal-size KPI panels

**Files:**
- Modify: `feature/home/src/main/java/com/lexora/service/feature/home/HomeScreen.kt`
- Test: `feature/home/src/androidTest/java/com/lexora/service/feature/home/OperationalSummaryLayoutTest.kt`

**Interfaces:**
- Consumes: current six KPI values and loading/error states.
- Produces: six equally sized cards in two rows; the Expected Payments card remains full-width and independently sized.

- [ ] Add Compose UI assertions comparing bounds for all six KPI cards and confirming the payments card spans the content width.
- [ ] Run the UI test and verify it fails with variable height for wrapped labels.
- [ ] Implement shared minimum/fixed card sizing and centered vertical content without clipping or truncating Russian labels.
- [ ] Run UI tests with short and wrapped labels and verify equal KPI bounds.
- [ ] Commit this task.

### Task 2: Regression verification

**Files:**
- Modify: `docs/LEXORA_SERVICE_CHANGELOG.md`

- [ ] Run `./gradlew :feature:home:test :feature:home:connectedAndroidTest :app:assembleDebug` on the configured Android runner.
- [ ] Verify Expected Payments remains a separate wide panel on narrow screens and with long values.
- [ ] Record results and commit the changelog update.
