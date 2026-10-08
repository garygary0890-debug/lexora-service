# 22 Lexora Debt Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide 11 Lexora Debt color families in light and dark modes on a dedicated profile theme screen.

**Architecture:** Add typed theme-family and appearance state, semantic ColorScheme mappings, persistence, a separate settings route, and profile entry. Keep selection and preview behavior consistent with Lexora Debt.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Android Gradle Plugin, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-08-service-user-workspaces-themes-design.md`

## Global Constraints

- Все изменения выполняются только в репозитории `garygary0890-debug/lexora-service`.
- Отдельные проекты и серверный репозиторий не изменяются.
- Доступ к данным пользовательской рабочей области получают её владелец и явно подключённые участники команды.
- Миграция Room не удаляет и не объединяет данные молча; неоднозначное сопоставление требует разрешения.
- Темы строятся на семантических цветовых токенах; геометрия компонентов между темами не меняется.

## Review Focus

1. A legacy/unknown stored theme key — test verifies deterministic safe fallback.
2. Toggling Light/Dark preserves the selected family — test asserts both dimensions independently.
3. Switching users preserves each user's selection — persistence test uses two user IDs.
4. Process recreation keeps the saved theme — UI/persistence test verifies round-trip.
5. Every family/mode pair resolves to a distinct supported scheme with adequate contrast — palette test checks all 22.

---

### Task 1: Theme model and color schemes

**Files:**
- Create: `core/model/src/main/java/com/lexora/service/core/model/ThemePreference.kt`
- Modify: `core/designsystem/src/main/java/com/lexora/service/core/designsystem/LexoraTheme.kt`
- Test: `core/designsystem/src/test/java/com/lexora/service/core/designsystem/LexoraThemeTest.kt`

**Interfaces:**
- Produces: `ThemeFamily` with Pearl, Graphite, Burgundy, Emerald, Sapphire, Amethyst, Petrol, Steel, Gold, Silver, Bronze; `AppearanceMode` LIGHT/DARK; `ThemePreference`.

- [ ] Add tests for exactly 11 families, two appearances per family, stable storage keys, and scheme lookup for all 22 combinations.
- [ ] Run the focused test and verify missing families/modes fail.
- [ ] Implement typed mapping to 22 semantic Material color schemes using the Lexora Debt palette values.
- [ ] Run tests and confirm no family/mode combination falls back to an unrelated palette.
- [ ] Commit this task.

### Task 2: Theme preference persistence and root application

**Files:**
- Create: `app/src/main/java/com/lexora/service/ThemePreferences.kt`
- Modify: `app/src/main/java/com/lexora/service/MainActivity.kt`
- Test: `app/src/test/java/com/lexora/service/ThemePreferencesTest.kt`

**Interfaces:**
- Consumes: `ThemePreference` and scheme mapping from Task 1.
- Produces: read/write preference API keyed by current user; root `LexoraTheme` follows persisted selection.

- [ ] Test round-trip persistence for every family and appearance and isolation between user IDs.
- [ ] Run the focused test and confirm missing persistence behavior fails.
- [ ] Implement preference serialization and root theme application without hard-coded colors in screens.
- [ ] Run theme preference tests and root app compilation.
- [ ] Commit this task.

### Task 3: Theme selection screen and profile route

**Files:**
- Create: `feature/settings/src/main/java/com/lexora/service/feature/settings/ThemeSettingsScreen.kt`
- Modify: `core/navigation/src/main/java/com/lexora/service/core/navigation/Routes.kt`
- Modify: `app/src/main/java/com/lexora/service/AppNavigationChrome.kt`, `MainActivity.kt`
- Test: `feature/settings/src/androidTest/java/com/lexora/service/feature/settings/ThemeSettingsScreenTest.kt`

**Interfaces:**
- Consumes: persistence and preference APIs from Task 2.
- Produces: a profile action «Настроить тему», a separate screen with 11 family choices and a Light/Dark switch, plus back navigation.

- [ ] Add UI assertions for 11 family choices, both modes, selection state, save/apply, and return navigation.
- [ ] Run the focused UI test and verify it fails before the screen and route exist.
- [ ] Add the profile action below user/team management and implement the dedicated selector screen.
- [ ] Run UI tests and verify selected theme survives navigation and process restart.
- [ ] Commit this task.

### Task 4: Theme contrast and full verification

**Files:**
- Test: `core/designsystem/src/test/java/com/lexora/service/core/designsystem/ThemeContrastTest.kt`
- Modify: `docs/LEXORA_SERVICE_CHANGELOG.md`

- [ ] Add contrast checks for primary text, cards, selected controls, warning/error states across 22 schemes.
- [ ] Run the contrast test and verify it detects a deliberately invalid semantic pairing.
- [ ] Correct palette/token values while preserving component geometry.
- [ ] Run `./gradlew :core:designsystem:test :feature:settings:test :app:assembleDebug` and update the changelog with actual results.
- [ ] Commit final verification records.
