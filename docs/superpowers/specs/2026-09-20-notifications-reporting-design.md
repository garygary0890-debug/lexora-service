# Lexora Service — Notifications and Reporting Design

Date: 2026-09-20
Status: Proposed for implementation
Scope: task reminders, push delivery, DND, SLA/contract/tire notifications, operational reports and analytics.

## 1. Goal

Close the remaining notification and reporting gaps without duplicating business rules in UI code. Notifications must be generated from domain state, delivered through a common delivery pipeline, respect per-user quiet hours, and remain auditable. Reports must be calculated from persisted application data rather than placeholder counters or repository contracts alone.

## 2. Architectural boundaries

The solution keeps feature UI free of repository orchestration. Domain components define notification events, delivery rules and report calculations. Data components persist notification state, user preferences, outbox items and reporting inputs. Android-specific delivery remains in the app/infrastructure layer. Server push is represented by a provider-neutral contract so FCM or another transport can be attached without changing domain rules.

Existing `NotificationRepository` remains the source for persisted in-app notifications, but generation is split into dedicated rule evaluators rather than a single growing method. Existing `ReportingRepository` becomes a real implementation backed by current Room/business repositories.

## 3. Notification model

Every generated notification has a stable idempotency key, organization, recipient user when applicable, type, priority, title, body, linked entity, due/scheduled time, creation time, delivery state and audit metadata. Re-generation updates the existing item rather than producing duplicates.

Required notification sources:
- task reminder at `reminderAtEpochMs`;
- SLA reaction warning/breach;
- SLA resolution warning/breach;
- contract expiry warning and expiry;
- tire storage reminder/overdue state;
- existing request deadlines and additional-work approvals.

Critical SLA breaches may bypass DND. Ordinary reminders are deferred until quiet hours end.

## 4. DND / quiet hours

Add per-user notification preferences with enabled flag, start/end local time, timezone, and critical-bypass flag. DND applies to system tray/push delivery, not to the in-app notification center: records remain visible immediately inside Lexora Service.

If a notification becomes due during DND, the delivery job stores/defers the next attempt until the quiet period ends. Time-window evaluation must support intervals crossing midnight.

## 5. Android delivery and push

Android local notifications use NotificationManager channels by priority/type and WorkManager for due-time delivery/retry. Android 13+ notification permission is handled explicitly.

Production push is provider-neutral:
- client registers a device token/installation id with backend when a provider is configured;
- backend push outbox stores idempotency key, recipient/device, payload, attempt count, next retry, final state and last error;
- provider adapter sends push and updates the outbox;
- retries are bounded with backoff;
- revoked/invalid tokens are disabled.

Initial implementation may ship with the outbox + provider interface and local Android notifications even when no concrete external push provider credentials exist. Such a build must not be described as external push production-ready until the provider is configured and end-to-end tested.

## 6. SLA notifications

Use the existing SLA reaction/resolution deadlines and warning window. Separate notification ids are retained for reaction and resolution. A warning escalates to breach with priority change and becomes unread again. Reaction notification closes after first reaction; resolution notification closes after request completion/cancellation.

SLA status in reports and notifications must be calculated by the same domain evaluator to avoid divergent business results.

## 7. Contract expiry notifications

For active contracts with an end date, generate warning notifications within a configurable lead time and a critical expiry notification once expired. Archived/cancelled/terminated contracts do not generate active reminders. Stable ids use contract id + warning/expired state.

## 8. Tire storage notifications

Tire storage records receive an expected storage end/pickup date and reminder lead time when not already represented. Active stored sets generate upcoming pickup/storage-expiry reminders and overdue alerts. Released/closed storage records automatically close related notifications.

## 9. Task reminders

`LinkedServiceTask` gains or uses a persisted reminder timestamp. Open/in-progress tasks generate reminders for their assignee; completed/cancelled tasks close pending reminder notifications. Editing the reminder reschedules the same stable notification rather than creating a second one.

## 10. Reporting architecture

Implement `PersistentReportingRepository` using persisted Service, business-operations, inventory and visit data. The report query accepts organization, branches and period. Calculations happen in domain/report calculators and return typed report models; the UI only renders them.

Required reports:
- SLA;
- executor workload;
- employee output/productivity;
- materials;
- revenue;
- accounts receivable/debt;
- repeat requests.

CSV export uses the same calculated dataset as the UI, not a separate formula path.

## 11. SLA report

Metrics: total applicable requests, met/missed reaction SLA, met/missed resolution SLA, currently at risk, average reaction time, average resolution time and breach rate. Filters: organization, branch and period. Open requests are evaluated at report generation time.

## 12. Executor workload report

Calculate planned minutes/hours per employee and team from scheduled visits/requests over the selected period, available working capacity where data exists, load percentage and overload flag. The same underlying planning data used by the calendar should feed this report.

## 13. Employee output report

Per employee: completed visits, completed work entries, total work quantity, completed requests attributable to the employee, productive visit duration where actual start/end are available, and optional revenue attribution only where a reliable link exists. Missing attribution must remain unassigned rather than guessed.

## 14. Materials report

Metrics by material and optionally warehouse: receipts, issues/write-offs, transfers in/out, reservations, released reservations, net movement, current quantity, reserved quantity and available quantity. Values are based on the inventory ledger and current balances.

## 15. Revenue report

Use posted/paid payment operations and net paid value after refunds. Provide gross payments, refunds and net revenue for the period. Planned/unpaid amounts are not revenue. If revenue cannot be reliably attributed to branch/employee, it remains in an unassigned bucket.

## 16. Debt report

For managed payments/invoices: amount due, net paid, outstanding amount, overdue amount when a due date exists, client/contract/order linkage, and aging buckets where due-date data is available. Records without a reliable due date remain outstanding but are not assigned to an overdue aging bucket.

## 17. Repeat-request analytics

A repeat request is determined by a documented deterministic rule: same client and same service object/equipment where available, created within a configurable repeat window after a previous closed request. Report includes repeat count, repeat rate, clients/assets with repeated requests, and top recurring problem/title groups. No semantic guessing beyond persisted identifiers and normalized exact/controlled fields.

## 18. UI

Notification settings add DND controls and channel toggles. Notification center displays source/type, priority, linked entity and delivery state where relevant.

Reports screen becomes a real report hub with period and branch filters, report selector, summary cards and tabular rows. It supports SLA, workload, employee output, materials, revenue, debt and repeat-request reports, plus CSV export.

## 19. Error handling and offline behavior

Report generation works fully from local persisted data. If sync is pending, UI shows that the report may not include remote changes yet. Notification generation also works offline. Push delivery requires connectivity and uses retry/outbox; local in-app and Android reminders remain functional without backend push.

## 20. Testing and completion criteria

Unit tests cover DND windows including midnight crossover, SLA warning→breach escalation, task reminder cancellation/reschedule, contract expiry, tire storage reminder closure, report formulas, payment refunds/net revenue, debt calculations and repeat-request window boundaries.

Integration tests cover notification persistence/idempotency, WorkManager scheduling, report repository aggregation and CSV consistency. Debug build must pass. External push is only marked production-ready after a configured provider, backend delivery test and physical-device receipt test; otherwise the completed scope is provider-ready outbox + local notification delivery.

## 21. Previous-stage completion

The implementation must retain and rebuild the previous completed flows: request scheduling/SLA, dispatch/routing, field visit execution/signature, inventory, procurement, payments and tasks. Any compile or behavior regression in those areas blocks completion of this stage.
