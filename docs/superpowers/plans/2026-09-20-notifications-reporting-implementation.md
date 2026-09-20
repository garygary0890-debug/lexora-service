# Notifications and Reporting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete task/SLA/contract/tire notifications, DND-aware local delivery and provider-ready push outbox, plus real SLA/workload/productivity/material/revenue/debt/repeat-request reports backed by persisted Lexora Service data.

**Architecture:** Domain calculators own notification timing and report formulas. Data repositories persist generated notifications, delivery preferences/outbox state and aggregate existing Room/business data. Android infrastructure schedules local delivery with WorkManager/NotificationManager; feature ViewModels render and edit state without repository orchestration. External push remains provider-neutral and is only called production-ready after a real provider is configured and device receipt is verified.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Lifecycle/ViewModel, Room, WorkManager, Android NotificationManager, existing Lexora DI/container, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-20-notifications-reporting-design.md`

## Global Constraints

- Existing feature UI must not create Room databases or repositories directly.
- Existing request scheduling/SLA, dispatch/routing, field visit/signature, inventory, procurement, payments and tasks must continue to compile and work.
- Generated notifications use stable idempotency keys and update existing records instead of duplicating them.
- DND suppresses/deferres Android/push delivery, not in-app notification visibility.
- Critical SLA breaches may bypass DND only when the preference explicitly permits it.
- Reports calculate from persisted data; no demo counters or inferred financial attribution.
- CSV export must use the same calculated dataset as on-screen reports.
- External push is not marked production-ready until a concrete provider, backend delivery and physical-device receipt are verified.

## Review Focus

- Quiet-hours window crossing midnight must defer ordinary delivery to the correct next local-time boundary.
- A warning escalating to an SLA breach must reuse the same logical event, increase priority and become unread again without duplication.
- Refunds and payment adjustments must not inflate revenue or understate debt.
- Branch/employee attribution must remain unassigned when persisted links are insufficient rather than guessed.
- Repeat-request analytics must respect the exact configurable window boundary and stable client/object/equipment identifiers.

---

### Task 1: Notification preference and delivery models

**Files:**
- Create: `core/model/src/main/java/com/lexora/service/core/model/NotificationDeliveryModels.kt`
- Modify: `core/model/src/main/java/com/lexora/service/core/model/NotificationModels.kt`
- Create: `core/domain/src/main/java/com/lexora/service/core/domain/QuietHoursPolicy.kt`
- Test: `core/domain/src/test/java/com/lexora/service/core/domain/QuietHoursPolicyTest.kt`

**Interfaces:**
- Produces: `NotificationPreferences`, `NotificationChannelPreference`, `NotificationDeliveryState`, `PushOutboxItem`, `QuietHoursPolicy.nextAllowedDelivery(preferences, nowEpochMs): Long`.

- [ ] **Step 1: Write failing quiet-hours tests**

```kotlin
@Test fun daytimeWindowDefersUntilEnd() {
    val prefs = NotificationPreferences(dndEnabled = true, dndStartMinute = 9 * 60, dndEndMinute = 17 * 60, timeZoneId = "UTC")
    assertEquals(epoch("2026-09-20T17:00:00Z"), QuietHoursPolicy.nextAllowedDelivery(prefs, epoch("2026-09-20T12:00:00Z")))
}

@Test fun midnightCrossingWindowDefersUntilMorning() {
    val prefs = NotificationPreferences(dndEnabled = true, dndStartMinute = 22 * 60, dndEndMinute = 7 * 60, timeZoneId = "UTC")
    assertEquals(epoch("2026-09-21T07:00:00Z"), QuietHoursPolicy.nextAllowedDelivery(prefs, epoch("2026-09-20T23:30:00Z")))
}
```

- [ ] **Step 2: Run the tests and verify failure**

Run: `./gradlew :core:domain:testDebugUnitTest --tests '*QuietHoursPolicyTest*'`
Expected: FAIL because the models/policy do not yet exist.

- [ ] **Step 3: Implement the models and pure quiet-hours policy**

```kotlin
data class NotificationPreferences(
    val userId: String = "",
    val dndEnabled: Boolean = false,
    val dndStartMinute: Int = 22 * 60,
    val dndEndMinute: Int = 7 * 60,
    val timeZoneId: String = "UTC",
    val allowCriticalBypass: Boolean = true,
    val taskRemindersEnabled: Boolean = true,
    val slaEnabled: Boolean = true,
    val contractEnabled: Boolean = true,
    val tireStorageEnabled: Boolean = true,
)

enum class NotificationDeliveryState { PENDING, DEFERRED, DELIVERED, FAILED, CANCELLED }
```

`QuietHoursPolicy` must validate minute values in `0..1439`, support equal start/end as no quiet window, convert using `ZoneId.of(timeZoneId)`, and return `nowEpochMs` when delivery is already allowed.

- [ ] **Step 4: Re-run tests**

Run: `./gradlew :core:domain:testDebugUnitTest --tests '*QuietHoursPolicyTest*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/model core/domain
git commit -m "feat: add notification delivery preferences and quiet hours"
```

### Task 2: Persist notification preferences and local delivery queue

**Files:**
- Create: `core/database/src/main/java/com/lexora/service/core/database/NotificationDeliveryEntities.kt`
- Create: `core/database/src/main/java/com/lexora/service/core/database/NotificationDeliveryDao.kt`
- Modify: `core/database/src/main/java/com/lexora/service/core/database/LexoraServiceDatabase.kt`
- Modify: `core/data/src/main/java/com/lexora/service/core/data/NotificationRepository.kt`
- Test: `core/data/src/test/java/com/lexora/service/core/data/NotificationDeliveryRepositoryTest.kt`

**Interfaces:**
- Consumes: models and `QuietHoursPolicy` from Task 1.
- Produces: persistence methods `preferences(userId)`, `savePreferences(value)`, `enqueueDelivery(...)`, `dueDeliveries(now)`, `markDeliveryState(...)`.

- [ ] **Step 1: Write Room/repository tests for idempotency and deferral**

```kotlin
@Test fun enqueueUsesStableIdempotencyKey() = runTest {
    repository.enqueueDelivery(event("task:42"))
    repository.enqueueDelivery(event("task:42"))
    assertEquals(1, repository.pendingDeliveries().size)
}

@Test fun ordinaryDeliveryInsideDndIsDeferred() = runTest {
    repository.savePreferences(quietNightPreferences())
    val row = repository.enqueueDelivery(event("contract:7", priority = WARNING))
    assertEquals(NotificationDeliveryState.DEFERRED, row.state)
    assertTrue(row.nextAttemptAtEpochMs > row.createdAtEpochMs)
}
```

- [ ] **Step 2: Verify tests fail**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*NotificationDeliveryRepositoryTest*'`
Expected: FAIL because entities/DAO are absent.

- [ ] **Step 3: Add Room entities and migration**

Create `notification_preferences` and `notification_delivery_queue` tables with unique `idempotencyKey`. Increment `LexoraServiceDatabase` version by one and add an explicit migration creating both tables and indexes on recipient/state/nextAttempt.

- [ ] **Step 4: Extend repository with persistence and DND evaluation**

Repository enqueue logic must compute `nextAttemptAtEpochMs` from `QuietHoursPolicy`, bypass only `CRITICAL` when `allowCriticalBypass=true`, and update an existing row on the same idempotency key.

- [ ] **Step 5: Run tests and database assembly**

Run: `./gradlew :core:data:testDebugUnitTest :core:database:assembleDebug`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/database core/data
git commit -m "feat: persist notification preferences and delivery queue"
```

### Task 3: Generate task, SLA, contract and tire-storage notifications

**Files:**
- Create: `core/domain/src/main/java/com/lexora/service/core/domain/NotificationRuleEngine.kt`
- Modify: `core/data/src/main/java/com/lexora/service/core/data/NotificationRepository.kt`
- Modify: business task model/entity file containing `LinkedServiceTask`
- Modify: tire storage model/entity file containing the persisted tire storage record
- Test: `core/domain/src/test/java/com/lexora/service/core/domain/NotificationRuleEngineTest.kt`

**Interfaces:**
- Produces: `NotificationRuleEngine.evaluate(input, nowEpochMs): List<GeneratedNotification>`.
- Stable keys: `task-reminder:<taskId>`, `sla-reaction:<requestId>`, `sla-resolution:<requestId>`, `contract-warning:<contractId>`, `contract-expired:<contractId>`, `tire-storage-warning:<storageId>`, `tire-storage-overdue:<storageId>`.

- [ ] **Step 1: Add failing rule tests**

Tests must cover: task reminder due only for OPEN/IN_PROGRESS; rescheduled task returns same stable key with new time; SLA warning escalates to CRITICAL breach; completed request removes active SLA candidate; active contract warns inside lead window and expires after end date; released tire storage produces no candidate.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :core:domain:testDebugUnitTest --tests '*NotificationRuleEngineTest*'`
Expected: FAIL.

- [ ] **Step 3: Persist missing reminder/storage timing fields**

Add `reminderAtEpochMs: Long?` and `assigneeUserId: String?` to the linked task model/entity if absent. Add `expectedPickupAtEpochMs: Long?` and `reminderLeadMinutes: Int` to tire-storage persistence if absent. Add explicit Room migrations for any existing database containing these entities.

- [ ] **Step 4: Implement `NotificationRuleEngine` and replace monolithic generation**

The engine receives typed requests/tasks/contracts/tire-storage records and returns immutable candidates. `NotificationRepository.refreshGenerated()` loads persisted inputs, calls the engine, upserts active candidates and archives generated records whose stable keys are no longer active. Reuse the existing SLA reaction/resolution deadline semantics.

- [ ] **Step 5: Fix existing mojibake notification strings while touching the generator**

Replace malformed encoded titles in `NotificationRepository.kt` with proper UTF-8 Russian strings and add a test asserting generated request-deadline text contains `Срок заявки`/`Просрочена заявка`.

- [ ] **Step 6: Run domain/data tests**

Run: `./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add core/domain core/data core/model core/database
git commit -m "feat: complete notification rule generation"
```

### Task 4: Android local notification scheduling and DND-aware delivery

**Files:**
- Create: `app/src/main/java/com/lexora/service/notifications/NotificationDeliveryWorker.kt`
- Create: `app/src/main/java/com/lexora/service/notifications/AndroidNotificationPublisher.kt`
- Create: `app/src/main/java/com/lexora/service/notifications/NotificationScheduler.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: app startup/composition root file where WorkManager jobs are installed
- Test: `app/src/test/java/com/lexora/service/notifications/NotificationSchedulerTest.kt`

**Interfaces:**
- Consumes: due delivery rows from Task 2.
- Produces: WorkManager scheduling and Android system notifications with channels `service_critical`, `service_warning`, `service_reminders`.

- [ ] **Step 1: Write scheduler tests**

Test that a due item schedules immediate work, a deferred item schedules at `nextAttemptAtEpochMs`, and duplicate idempotency keys map to the same unique work name `notification:<deliveryId>`.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*NotificationSchedulerTest*'`
Expected: FAIL.

- [ ] **Step 3: Implement channel creation and Android 13+ permission handling**

Manifest adds `android.permission.POST_NOTIFICATIONS`. Publisher creates channels once, uses priority mapping, includes linked entity metadata in the pending intent, and does not throw if permission is denied.

- [ ] **Step 4: Implement worker and scheduling**

Worker rechecks current preferences before publishing. If DND now applies, update row to `DEFERRED` and schedule the next allowed attempt. On success mark `DELIVERED`; on retryable failure increment attempt count and return retry with bounded backoff.

- [ ] **Step 5: Run app tests and compile**

Run: `./gradlew :app:testDebugUnitTest :app:compileDebugKotlin`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src
git commit -m "feat: deliver local notifications with WorkManager"
```

### Task 5: Provider-neutral push outbox contract

**Files:**
- Create: `core/network/src/main/java/com/lexora/service/core/network/PushDeliveryApi.kt`
- Create: `core/data/src/main/java/com/lexora/service/core/data/PushOutboxRepository.kt`
- Modify: `app/src/main/java/com/lexora/service/di/LexoraServiceContainer.kt`
- Test: `core/data/src/test/java/com/lexora/service/core/data/PushOutboxRepositoryTest.kt`

**Interfaces:**
- Produces: `PushProvider.send(payload): PushSendResult`, device registration contract, idempotent outbox enqueue/retry/disable-token behavior.

- [ ] **Step 1: Write failing outbox tests**

Tests cover same idempotency key not duplicating, retry increments attempt count and computes later retry, permanent invalid-token result disables the device registration, and DND-deferral does not call provider.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*PushOutboxRepositoryTest*'`
Expected: FAIL.

- [ ] **Step 3: Implement provider interface and outbox repository**

Do not add Firebase credentials. The default provider implementation is `NoConfiguredPushProvider`, which returns a typed `NOT_CONFIGURED` result without falsely marking an event delivered.

- [ ] **Step 4: Register dependencies and expose status in notification settings**

DI provides the outbox/provider. UI can display `Push: провайдер не настроен` rather than claiming production push readiness.

- [ ] **Step 5: Run tests/compile**

Run: `./gradlew :core:data:testDebugUnitTest :core:network:assembleDebug :app:compileDebugKotlin`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/network core/data app
git commit -m "feat: add provider-neutral push outbox"
```

### Task 6: Notification center and DND settings UI

**Files:**
- Modify: `feature/notifications/src/main/java/com/lexora/service/feature/notifications/NotificationsViewModel.kt`
- Modify: `feature/notifications/src/main/java/com/lexora/service/feature/notifications/NotificationsScreen.kt`
- Modify: domain notification operations contract used by the ViewModel
- Test: `feature/notifications/src/test/java/com/lexora/service/feature/notifications/NotificationsViewModelTest.kt`

**Interfaces:**
- Consumes: repository preferences, generated notifications and push/provider status.
- Produces: editable DND/channel state and notification list showing type, priority, linked entity and delivery state.

- [ ] **Step 1: Write ViewModel tests**

Tests cover loading saved preferences, rejecting invalid `HH:mm`, saving `22:00–07:00`, and preserving in-app visibility when DND is enabled.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :feature:notifications:testDebugUnitTest`
Expected: FAIL.

- [ ] **Step 3: Extend ViewModel state/actions**

Add `preferences`, `pushStatus`, `saveDnd(enabled,start,end,criticalBypass)`, and per-channel toggles. No database access from Composable.

- [ ] **Step 4: Extend screen**

Add a compact settings section for DND start/end, critical bypass and Task/SLA/Contracts/Tires toggles. Notification cards display source/entity and delivery state where applicable.

- [ ] **Step 5: Run tests and feature compile**

Run: `./gradlew :feature:notifications:testDebugUnitTest :feature:notifications:assembleDebug`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add feature/notifications core/domain
git commit -m "feat: add notification preferences and delivery UI"
```

### Task 7: Typed reporting models and calculators

**Files:**
- Create: `core/model/src/main/java/com/lexora/service/core/model/OperationalReportModels.kt`
- Create: `core/domain/src/main/java/com/lexora/service/core/domain/ReportingCalculators.kt`
- Test: `core/domain/src/test/java/com/lexora/service/core/domain/ReportingCalculatorsTest.kt`

**Interfaces:**
- Produces: `SlaReport`, `ExecutorWorkloadReport`, `EmployeeOutputReport`, `MaterialsReport`, `RevenueReport`, `DebtReport`, `RepeatRequestReport`, plus shared `ReportFilter`.

- [ ] **Step 1: Write failing calculator tests**

Tests must pin: SLA reaction/resolution met/missed/at-risk; workload percentage and overload; employee visit/work counts/duration; material receipt/issue/reservation/availability; net revenue = payments - refunds; outstanding debt = amount due - net paid; repeat request exactly on the configured window boundary counts consistently.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :core:domain:testDebugUnitTest --tests '*ReportingCalculatorsTest*'`
Expected: FAIL.

- [ ] **Step 3: Implement pure calculators**

Calculators accept typed input rows and `ReportFilter`. Financial calculators use payment-operation timestamps for period inclusion. Workload reuses planned visit durations. Unknown branch/employee attribution maps to `null`/`Unassigned`, never a guessed owner.

- [ ] **Step 4: Run tests**

Run: `./gradlew :core:domain:testDebugUnitTest --tests '*ReportingCalculatorsTest*'`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add core/model core/domain
git commit -m "feat: add operational reporting calculators"
```

### Task 8: Implement `PersistentReportingRepository` and CSV export

**Files:**
- Modify: `core/data/src/main/java/com/lexora/service/core/data/ReportingRepository.kt`
- Create: `core/data/src/main/java/com/lexora/service/core/data/PersistentReportingRepository.kt`
- Modify: `app/src/main/java/com/lexora/service/di/LexoraServiceContainer.kt`
- Test: `core/data/src/test/java/com/lexora/service/core/data/PersistentReportingRepositoryTest.kt`

**Interfaces:**
- Consumes: service DAO, visit/work/material data, inventory repository, business payment/task data and calculators from Task 7.
- Produces: typed report methods and `exportCsv(reportCode, filter): ByteArray` derived from the same report result.

- [ ] **Step 1: Write integration-style repository tests with deterministic fixtures**

Fixtures include two employees, three requests with one SLA breach, one refund, one open debt, receipt/write-off/reservation ledger rows and one repeat request. Assert report totals and CSV totals match.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :core:data:testDebugUnitTest --tests '*PersistentReportingRepositoryTest*'`
Expected: FAIL.

- [ ] **Step 3: Expand `ReportingRepository` into typed methods**

```kotlin
suspend fun sla(filter: ReportFilter): SlaReport
suspend fun workload(filter: ReportFilter): ExecutorWorkloadReport
suspend fun employeeOutput(filter: ReportFilter): EmployeeOutputReport
suspend fun materials(filter: ReportFilter): MaterialsReport
suspend fun revenue(filter: ReportFilter): RevenueReport
suspend fun debt(filter: ReportFilter): DebtReport
suspend fun repeatRequests(filter: ReportFilter): RepeatRequestReport
suspend fun exportCsv(reportCode: ReportCode, filter: ReportFilter): ByteArray
```

Keep the old `snapshot(...)` only as a compatibility adapter until all callers are migrated.

- [ ] **Step 4: Implement data gathering and CSV serialization**

Use existing DAO/repository APIs; add focused DAO queries only where loading all rows would create incorrect period filtering. CSV header/rows come from the typed report result and use UTF-8 with deterministic column order.

- [ ] **Step 5: Register `PersistentReportingRepository` in DI**

`LexoraServiceContainer` exposes the interface, not the concrete repository, to feature code.

- [ ] **Step 6: Run tests and core assembly**

Run: `./gradlew :core:data:testDebugUnitTest :core:data:assembleDebug`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add core/data core/database app/src/main/java/com/lexora/service/di
git commit -m "feat: implement persisted operational reports"
```

### Task 9: Replace report placeholders with the report hub UI

**Files:**
- Modify: `feature/reports/src/main/java/com/lexora/service/feature/reports/ReportsViewModel.kt`
- Modify: `feature/reports/src/main/java/com/lexora/service/feature/reports/ReportsScreen.kt`
- Test: `feature/reports/src/test/java/com/lexora/service/feature/reports/ReportsViewModelTest.kt`

**Interfaces:**
- Consumes: typed `ReportingRepository` methods from Task 8.
- Produces: report selector, period/branch filter state, summary cards, detail rows and CSV export event.

- [ ] **Step 1: Write ViewModel tests**

Test switching between all seven report codes, changing period reloading the active report, branch filters propagating into `ReportFilter`, and export calling the active report code with the same filter.

- [ ] **Step 2: Verify failure**

Run: `./gradlew :feature:reports:testDebugUnitTest`
Expected: FAIL.

- [ ] **Step 3: Implement ViewModel state**

State contains `selectedReport`, `fromEpochMs`, `toEpochMs`, `branchIds`, `loading`, `error`, typed/current display rows, and `dataFreshnessWarning` when pending sync exists.

- [ ] **Step 4: Implement report hub UI**

Use a report selector for `SLA / Загрузка / Выработка / Материалы / Выручка / Задолженность / Повторные обращения`, reusable period/branch controls, summary cards and a scrollable detail table. Keep CustomerCare separate from these management reports.

- [ ] **Step 5: Run feature tests/assembly**

Run: `./gradlew :feature:reports:testDebugUnitTest :feature:reports:assembleDebug`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add feature/reports
git commit -m "feat: complete operational reports UI"
```

### Task 10: Full regression verification and stage closure

**Files:**
- Modify if needed: `.github/workflows/verify-fieldwork-inventory-once.yml`
- Create: `docs/verification/2026-09-20-notifications-reporting.md`

**Interfaces:**
- Validates all new and previous-stage components together.

- [ ] **Step 1: Run focused unit suites**

Run:
```bash
./gradlew :core:domain:testDebugUnitTest :core:data:testDebugUnitTest :feature:notifications:testDebugUnitTest :feature:reports:testDebugUnitTest
```
Expected: all tests PASS.

- [ ] **Step 2: Run full debug build including prior feature modules**

Run:
```bash
./gradlew --no-daemon --stacktrace :core:database:assembleDebug :core:data:assembleDebug :core:network:assembleDebug :feature:planning:assembleDebug :feature:fieldwork:assembleDebug :feature:notifications:assembleDebug :feature:reports:assembleDebug :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 3: Add/adjust CI verification**

Ensure the verification workflow runs the same focused tests and full build, uploads the APK, and is triggered by the relevant notification/report/core file paths rather than only by edits to the workflow file itself.

- [ ] **Step 4: Write factual verification record**

Document exact commit SHA, Gradle commands/results, APK path/hash, tests executed, and explicitly state external-push status. Use `provider-ready, not production-verified` unless a concrete provider and physical-device push receipt have actually been tested.

- [ ] **Step 5: Commit stage closure**

```bash
git add .github/workflows docs/verification
git commit -m "test: verify notifications reporting and prior service flows"
```

- [ ] **Step 6: Final repository check**

Run: `git status --short && git log -5 --oneline`
Expected: clean tracked tree and the task commits present in order.

## Self-review result

- Spec coverage: all notification sources, DND, local delivery, provider-ready push, seven requested reports, CSV and previous-stage regression build are mapped to tasks.
- Placeholder scan: no `TBD`, `TODO`, `implement later`, or undefined implementation steps remain.
- Type consistency: notification preferences/outbox are established before delivery/UI; report models/calculators are established before repository/UI.
- Review Focus coverage: midnight DND is Task 1, SLA escalation Task 3, refund/net revenue Task 7, unassigned attribution Task 7/8, repeat-window boundary Task 7.
