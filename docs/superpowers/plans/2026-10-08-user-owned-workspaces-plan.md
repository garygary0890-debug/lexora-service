# User-Owned Workspaces and Team Access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Organization as an active entity from Lexora Service and scope shared service data to owner users and explicit team membership.

**Architecture:** Replace organization ownership with owner-user ownership across Room, domain, data, sync and UI. Convert team roles to user-to-user workspace membership, preserve old data through a guarded versioned migration, and remove organization routes and creation/switching flows.

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

### Task 1: User workspace membership and authorization

**Files:**
- Modify: `core/database/src/main/java/com/lexora/service/core/database/UserEntities.kt`
- Modify: `core/model/src/main/java/com/lexora/service/core/model/Organization.kt` and user/role models in `core/model/src/main/java/com/lexora/service/core/model/`
- Modify: `core/domain/src/main/java/com/lexora/service/core/domain/` access policies and user/workspace operations
- Test: `core/domain/src/test/java/com/lexora/service/core/domain/UserWorkspaceAccessTest.kt`

**Interfaces:**
- Produces: a typed membership/access model keyed by `ownerUserId`, `memberUserId`, and role; an authorization decision for read/write by user and owner.

- [x] Write tests proving owner access, granted team-member access, denied non-member access, and role-limited writes.
- [x] Run the new domain test and confirm it fails on the absent user-workspace policy.
- [x] Implement the user membership model and policy.
- [x] Run the focused domain tests and confirm all access cases pass.
- [x] Commit this task.

### Task 2: Room ownership migration

**Files:**
- Modify: `core/database/src/main/java/com/lexora/service/core/database/Entities.kt`, `BusinessOperationsEntities.kt`, `CatalogEntities.kt`, `ContractEntities.kt`, `CustomerCareEntities.kt`, `InventoryEntities.kt`, `PublicPortalEntities.kt`, `ReferenceDataEntities.kt`, `ServiceConstructorEntities.kt`, `WorkOrderEntities.kt`, `UserEntities.kt`
- Modify: `core/database/src/main/java/com/lexora/service/core/database/LexoraServiceDatabase.kt` (schema version 22 → 23)
- Modify affected DAO files under `core/database/src/main/java/com/lexora/service/core/database/`
- Test: `core/database/src/test/java/com/lexora/service/core/database/UserWorkspaceMigrationTest.kt`

**Interfaces:**
- Consumes: user membership model from Task 1.
- Produces: schema with `ownerUserId` fields and no active organization table/role entity.

- [ ] Add Room migration tests from schema v22 for unique owner mapping, multiple team members, retained links/audit/sync states, request-number conflicts, and ambiguous mappings.
- [ ] Run focused migration tests and verify failure on v22 schema before implementation.
- [ ] Implement transactional v22→v23 conversion of all business-table ownership. The current migration only copies unambiguous membership rows, records ambiguous owners, and preserves old rows.
- [ ] Run migration tests on both valid and ambiguous fixtures; verify the legacy tables remain intact on the ambiguous case.
- [ ] Commit this task.

### Task 3: Repositories, use cases, and sync scope

**Files:**
- Modify organization-dependent files in `core/data/src/main/java/com/lexora/service/core/data/`, including `OrganizationSessionRepository.kt`, `PersistentOrganizationRepository.kt`, and `PersistentUserRepository.kt`
- Modify ownership/access operations in `core/domain/src/main/java/com/lexora/service/core/domain/`
- Modify `app/src/main/java/com/lexora/service/di/LexoraServiceContainer.kt`, `LexoraBackendGraph.kt`, and sync/auth files where organization scope is consumed
- Test: `core/data/src/test/java/com/lexora/service/core/data/UserWorkspaceRepositoryTest.kt`

**Interfaces:**
- Consumes: migrated `ownerUserId` and membership policy from Tasks 1–2.
- Produces: repositories that accept the active user/workspace owner identity and enforce membership before reads/writes.

- [ ] Add tests for cross-user reads/writes, membership revocation, offline queue owner scope, and API payload ownership.
- [ ] Run focused tests and confirm they fail against organization-scoped repository contracts.
- [ ] Update repositories, audit events, module settings, and sync scopes to use user ownership and explicit memberships. Blocked by the current backend's required organizationId/login header/sync routes; mismatch is recorded in the spec.
- [ ] Run focused data/domain tests; verify backend incompatibilities surface as errors.
- [ ] Commit this task.

### Task 4: Organization screens and app navigation removal

**Files:**
- Modify: `core/navigation/src/main/java/com/lexora/service/core/navigation/Routes.kt`
- Modify: `app/src/main/java/com/lexora/service/MainActivity.kt`, `AppNavigationChrome.kt`
- Modify or remove: `feature/organization/src/main/java/com/lexora/service/feature/organization/` and module registration in `settings.gradle.kts`
- Modify: settings/profile copy and `docs/LEXORA_SERVICE_CHANGELOG.md`
- Test: `core/navigation/src/test/java/com/lexora/service/core/navigation/RoutesTest.kt`

**Interfaces:**
- Consumes: user-owned repository contract from Task 3.
- Produces: navigation/profile flow with user/team management, no organization screen, creation, selector, or user-visible organization copy.

- [ ] Add route tests proving removed organization routes are unavailable and profile opens user/team management.
- [ ] Run the focused navigation tests and confirm they fail for current organization routes.
- [ ] Remove organization flows and replace visible copy with the approved user/team wording; retain old table names only in migration code/history.
- [ ] Run navigation tests and verify no active app route refers to organizations.
- [ ] Commit this task.

### Task 5: Full migration and build verification

**Files:**
- Modify: `.github/workflows/build.yml` to run on `feature/srv-104-user-workspace-themes` and execute affected module unit-test tasks before assembling the APK
- Modify: `docs/LEXORA_SERVICE_CHANGELOG.md`, `build-diagnostics/latest-summary.txt`
- Verify: all modules in the Lexora Service repository.

- [ ] Run searches for active `Organization`, `organizationId`, organization routes, strings, and API fields; distinguish migration-only historical references from active code.
- [ ] Run the affected module `testDebugUnitTest` tasks and `:app:assembleDebug` on the GitHub Actions Ubuntu/JDK 17/Gradle 9.4.1 runner; record full results.
- [ ] Run Room schema validation and verify user-team access remains isolated.
- [ ] Update the changelog and build diagnostic with actual status.
- [ ] Commit final verification records.
