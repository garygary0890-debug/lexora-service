# Lexora Service Full Service Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining Lexora Service production gaps as tested end-to-end flows, starting with stabilization and the previously approved notification/reporting work.

**Architecture:** Existing module boundaries remain authoritative: model/domain/data/database/network/presentation/feature/app. Business invariants live in domain/data and are enforced again by backend for shared/server-controlled concerns. UI renders typed state only. Server-side tenancy/RBAC/licensing/booking overlap are never replaced by client-only checks.

**Tech Stack:** Kotlin, Jetpack Compose, Room, WorkManager, Android Keystore, Gradle, existing Lexora backend REST stack, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-20-full-service-completion-design.md`

## Global Constraints
- Latest explicit user instruction > approved TЗ > Lexora family contracts > project regulations > architecture docs > journals/tests.
- No repository/interface alone counts as completion.
- Current revision must build before an implementation stage is called complete.
- Production push requires a configured provider plus end-to-end receipt test; provider-ready outbox is reported separately.
- Backend remains authoritative for RBAC, tenancy, entitlements and booking conflicts.
- Closed work-order commercial data and signed/approval events are immutable or versioned.
- Offline operations must remain idempotent and migration-safe.

## Review Focus
- Cross-midnight DND and timezone changes must never drop a critical alert or deliver an ordinary alert during quiet hours.
- Two concurrent bookings for the same branch/post/time must yield one accepted booking and one deterministic conflict, including server-side enforcement.
- Closed-order prices must remain unchanged after price-list edits/imports.
- Tenant/branch ownership must be verified on every server mutation/read introduced by this program.
- Offline replay after reconnect must not duplicate payments, inventory moves, approvals, bookings, or signed events.

---

### Task 1: Current-main baseline and regression gate
**Files:** Modify `.github/workflows/build.yml`; create/update focused tests only as failures demand; update `build-diagnostics` after verified build.
**Interfaces:** Produces a repeatable current-revision verification command and build artifact used by every later task.
- [ ] Add CI steps for unit tests, migration tests where present, and `:app:assembleDebug` before APK upload.
- [ ] Run baseline on branch and capture exact failures.
- [ ] Fix only current-main compile/test regressions, each with a reproducing test where behavior is changed.
- [ ] Verify previous request/SLA, dispatch/routing, fieldwork/signature, inventory, procurement, payments and task modules compile in the full graph.
- [ ] Commit baseline fixes and record verified commit/run.

### Task 2: Notification policy, reminders, DND and local delivery
**Files:** Modify `core/model` notification/task models, `core/domain` notification operations, `core/database` entities/DAO/migration, `core/data/NotificationRepository.kt`; create policy/evaluator tests; modify `feature/notifications`; add Android delivery worker/channel manager in app infrastructure.
**Interfaces:** Produces `NotificationPreferences`, quiet-hours evaluator, stable generated notification ids, due delivery jobs and typed delivery state.
- [ ] RED tests: cross-midnight DND, critical bypass, task reminder due/reschedule/cancel, SLA warning→breach, contract expiry close, tire-storage close.
- [ ] Implement persisted per-user quiet hours/channel preferences and migration.
- [ ] Split generated-event evaluation by source while preserving idempotent notification ids.
- [ ] Add local Android delivery through WorkManager/NotificationManager and Android 13+ permission handling.
- [ ] Extend notification UI with DND/channel controls and delivery/source state.
- [ ] Run focused tests then full suite/build; commit.

### Task 3: Provider-neutral push outbox
**Files:** Create push outbox model/entity/DAO/repository/provider contract and retry worker; add backend-facing registration/outbox API adapter when current server contract supports it.
**Interfaces:** Produces idempotent enqueue, retry/backoff, terminal failure, token-disable behavior; does not claim external-provider production readiness without credentials/device test.
- [ ] RED tests for duplicate enqueue, retry schedule, terminal failure, invalid-token disable and DND deferral.
- [ ] Implement persisted outbox and provider interface.
- [ ] Wire notification generation to outbox for eligible channels.
- [ ] Add audit/error state surfaced in notification center.
- [ ] Verify tests/full build; commit.

### Task 4: Real reporting repository and report hub
**Files:** Replace contract-only `ReportingRepository` with persistent implementation; add typed report models/calculators in domain/model; modify reports ViewModel/screen; add CSV consistency tests.
**Interfaces:** Produces SLA, workload, employee output, materials, revenue, debt and repeat-request datasets from persisted data with organization/branch/period filters.
- [ ] RED formula tests for every report including refunds/net revenue, overdue debt and repeat-window boundary.
- [ ] Implement typed calculators using the same SLA evaluator as notifications.
- [ ] Implement `PersistentReportingRepository.snapshot/exportCsv` from actual Room/business/inventory/visit data.
- [ ] Wire report selector, period/branch filters, summary cards/table rows and CSV export.
- [ ] Verify focused tests/full build; commit.

### Task 5: Work-order commercial core and immutable approvals
**Files:** Extend work-order/domain/database models and migrations; modify repository/ViewModel/screens; audit integration.
**Interfaces:** Produces frozen order price snapshot, versioning, close lock, immutable additional-work accept/decline event and QC gate.
- [ ] RED tests for price freeze, closed mutation rejection, version increment, approval immutability and QC close gate.
- [ ] Implement versioned/frozen order model and adjustment path.
- [ ] Implement send/accept/decline additional-work events with actor/channel/time/payload hash.
- [ ] Complete full create→work/materials→approval→QC→payment→close UI flow.
- [ ] Add audit and regression tests; commit.

### Task 6: Branch-aware price lists, contracts and service history
**Files:** Extend price/contract/history models, repositories, DB migrations, screens and import/export adapters.
**Interfaces:** Produces branch/client-type/season/module price selection with validity/history, full contract scope/terms/SLA/limits/versioned signed docs, unified cross-module chronology.
- [ ] RED tests for effective price selection, validity boundaries, branch separation, closed-order invariance, contract scope and chronology ordering.
- [ ] Implement price history/import/export and audit.
- [ ] Implement contract links to branch/object/vehicle/request/order/invoice/payment and signed versions.
- [ ] Merge repair/wash/tire/docs/payments/order events into unified vehicle service history.
- [ ] Verify tests/build; commit.

### Task 7: Complete car-wash flow
**Files:** Extend wash models/repository/database/feature screens; backend booking API changes in `lexora-backend` tracked by matching contract tests.
**Interfaces:** Produces post state machine, booking conflict enforcement, wash order lifecycle, packages, class pricing, discount/surcharge snapshot, tech cards, chemical norm/fact/variance and QC.
- [ ] RED tests for all post transitions and invalid transitions.
- [ ] RED concurrent-overlap test at repository and backend API levels.
- [ ] Implement booking and wash order lifecycle from booking to paid/closed.
- [ ] Implement packages/vehicle class pricing/discount/surcharge with frozen commercial snapshot.
- [ ] Implement technology cards and normative chemical demand.
- [ ] Post actual chemical use to inventory; calculate/report variance and excess.
- [ ] Add QC close gate; run E2E wash scenario; commit Android and backend changes.

### Task 8: Complete tire-service and storage flow
**Files:** Extend tire models/repository/database/screens and attachments; add storage contract/location/move/label models.
**Interfaces:** Produces tire order lifecycle, master/post assignment, 1/2/4 wheel operations, multiple sets, per-wheel diagnostics and managed storage.
- [ ] RED tests for wheel-count validation, multiple sets, per-wheel diagnostics, storage extension, location moves and release.
- [ ] Implement booking/order/post/master flow.
- [ ] Implement wheel cards: tread, pressure, rim, manufacturer/model, production date, defects and attachment ids.
- [ ] Implement storage agreement, expected end/extensions, physical location entity and move history.
- [ ] Implement QR/barcode label id and lookup workflow.
- [ ] Run tire E2E including storage/pickup; commit.

### Task 9: Loyalty, fault analytics and unified customer value
**Files:** Extend customer-care models/repository/database/reports/UI.
**Interfaces:** Produces nonnegative bonus ledger, earn/redeem/expire/adjust rules, tiers/limits and loyalty/fault analytics.
- [ ] RED tests for earn eligibility, redemption limits, negative balance rejection, expiry, tier boundaries and rule dates.
- [ ] Implement bonus account ledger and rule engine.
- [ ] Implement loyalty UI and reporting.
- [ ] Implement deterministic vehicle/equipment fault analytics from stored codes/identifiers.
- [ ] Verify tests/build; commit.

### Task 10: Portal/online booking/queue-display foundations
**Files:** Android client contracts where needed; backend versioned API endpoints, DTOs, auth/tenant/entitlement guards and tests.
**Interfaces:** Produces secure `/api/v1` portal foundation, online booking and read-only queue display API.
- [ ] RED API tests for unauthenticated, wrong tenant, wrong branch, missing entitlement and booking conflict.
- [ ] Implement whitelisted portal read/write endpoints.
- [ ] Implement server-side online-booking validation/conflict protection.
- [ ] Implement purpose-limited read-only queue display endpoint.
- [ ] Verify backend tests and Android contract compile; commit.

### Task 11: Integration platform, webhooks and import/export
**Files:** Extend integration repository/model/backend outbox/API/error journal; CSV/XLSX adapters and UI flows.
**Interfaces:** Produces versioned external API, signed/idempotent webhook delivery with retries, audited imports/exports and row-level validation results.
- [ ] RED tests for webhook retry/idempotency/signature and terminal failure.
- [ ] Implement versioned external API compatibility envelope.
- [ ] Implement webhook outbox/retry/error journal.
- [ ] Implement CSV/XLSX preview/validate/apply/export for price lists and supported datasets.
- [ ] Add import/export audit; verify tests/build; commit.

### Task 12: File/document platform
**Files:** File metadata/version models, backend storage endpoints, Android upload/download workers, document UI actions.
**Interfaces:** Produces checksum/versioned metadata, resumable upload, signed-document immutability, PDF/print/share hooks.
- [ ] RED tests for checksum mismatch, resume offsets, signed-version mutation rejection and version history.
- [ ] Implement server file metadata/storage contract and resumable chunks.
- [ ] Implement background Android upload/download with retry.
- [ ] Wire document import/export, PDF generation where required, system print/share.
- [ ] Verify tests/build; commit.

### Task 13: Server-authoritative RBAC, tenancy and module licensing
**Files:** `lexora-backend` permission/tenant/entitlement middleware and tests; Android consumes effective access only.
**Interfaces:** Produces authoritative deny/allow decisions by org/branch/object/operation/ownership and module entitlement.
- [ ] RED API tests for each permission dimension and cross-tenant access.
- [ ] RED tests for module API denial without entitlement and allow after grant.
- [ ] Implement enforcement centrally and cover all Service endpoints introduced above.
- [ ] Audit role/right/license changes.
- [ ] Verify backend suite; commit.

### Task 14: Sync, migrations, background work and environment hardening
**Files:** Sync engine/queue tests, Room migrations, environment config/health, background workers; backend health/migration/rollback docs/tests.
**Interfaces:** Produces idempotent offline replay, environment-specific secure endpoints, health checks and migration evidence.
- [ ] RED sync tests for duplicate replay, conflict, retry and partial failure.
- [ ] Add migration tests for every schema version introduced by this program.
- [ ] Verify Keystore token storage/device binding and TLS-only production configuration.
- [ ] Add dev/test/prod endpoint separation and health-check consumption.
- [ ] Verify background sync/uploads/notifications; commit.

### Task 15: UX/accessibility/localization/navigation regression
**Files:** touched feature resources/screens/navigation state tests.
**Interfaces:** Produces resources for touched strings, touch targets, large-font handling, theme contrast checks, bulk selection contracts and back/scroll restoration.
- [ ] Add UI tests for selection-back behavior and scroll restoration on high-risk screens.
- [ ] Move touched hardcoded strings to resources.
- [ ] Audit minimum touch target and large-font behavior for new/changed screens.
- [ ] Validate light/dark themes and bulk actions where required.
- [ ] Verify UI test subset/full build; commit.

### Task 16: Release gate and end-to-end acceptance
**Files:** release Gradle/signing config references, CI workflows, build diagnostics, release checklist.
**Interfaces:** Produces traceable signed release candidate and acceptance evidence.
- [ ] Run full unit/repository/sync/migration/API/RBAC/UI regression suites.
- [ ] Run core E2E and offline→reconnect→sync-no-duplicates.
- [ ] Run full wash and tire E2E.
- [ ] Create production backup/migration/rollback rehearsal evidence on non-production first.
- [ ] Build signed release, install over previous supported APK, verify data preservation and smoke critical flows.
- [ ] Record version/commit/build hashes and only then mark release-ready.
