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

1. Overflow actions are shown only on the Menu screen — UI test checks Menu and Home.
2. The editor is dismissed with Back — UI test verifies return to Menu without losing preferences.
3. Three-section limit and route validation — test verifies invalid routes are ignored and only three extras persist.
4. User-specific preference storage — test verifies users do not overwrite each other's menu selection.
5. Reordered sections survive app navigation — UI test verifies saved order after returning to Home and Menu.

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

- [ ] Run `./gradlew :app:test :app:connectedDebugAndroidTest :app:assembleDebug` on the configured Android runner.
- [ ] Verify other top-bar actions remain available on screens where they belong.
- [ ] Record results and commit the changelog update.
