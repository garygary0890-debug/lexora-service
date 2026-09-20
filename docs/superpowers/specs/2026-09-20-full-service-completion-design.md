# Lexora Service — Full Service Completion Design

Date: 2026-09-20
Status: Approved
Base: main @ 8f6df2d9b94837225b8698e38d04b9fe58e6e9de

## Goal
Close the remaining Lexora Service gaps as working end-to-end product flows rather than repository contracts. A requirement is complete only when persisted data, domain rules, UI/API, audit where required, automated verification, and an actual build are all present.

## Completion rule
No repository/interface alone is evidence of completion. Each completed capability must include: persistent model/data layer; domain rule/use case; ViewModel/UI or server API; branch/organization ownership checks where applicable; immutable/audited critical events; tests for business rules; integration verification; current-revision build evidence.

## Program order
1. Stabilization plus notifications/reporting already approved in `2026-09-20-notifications-reporting-design.md`.
2. Commercial core: work orders, approvals, QC, service history, prices, contracts, branches.
3. Car wash: booking to payment/close, posts, packages, vehicle-class pricing, adjustments, tech cards, chemical norm/fact, QC.
4. Tire service/storage: booking/orders/posts/masters, wheel-level diagnostics, storage contracts/locations/moves/labels.
5. Loyalty and analytics: bonus ledger, rules/tiers/limits, reporting, fault and repeat-request analytics, unified chronology.
6. External/platform: portal foundation, online booking API, queue display read API, versioned external API, webhooks/retries/errors, CSV/XLSX and document import/export, file storage/versioning/resume.
7. Security/release gate: server RBAC and tenancy, module entitlement enforcement, Keystore/device binding/TLS/environments/health, backups/migrations/rollback, background jobs, accessibility/localization/navigation contracts, release signing/update/smoke/E2E.

## Notifications and reporting
Implement task reminders, DND/quiet hours with critical bypass, local Android notification delivery, provider-neutral push outbox, SLA reaction/resolution warning/breach, contract expiry, tire storage reminders, and real reports for SLA, workload, employee output, materials, revenue, debt, repeat requests. External push is only production-ready after a configured provider and physical-device receipt test.

## Commercial core
A work order freezes commercial terms used for the order. Closed work-order financial/work lines are immutable; subsequent corrections create versions or explicit adjustments. Additional-work approvals are immutable events with sent/accepted/declined timestamps, actor/channel and payload hash. QC is a required close gate when policy demands it. Unified service history combines repair/service request, wash, tire, documents, payments and work-order events ordered by occurred time.

Price rules include organization, optional branch, client type, season, module, valid-from/to, version/history and import/export. Contract terms include validity, branch/object/vehicle scope, payment terms, special price/discount rules, SLA, limits and signed document versions. Closed-order prices never recalculate from later price-list changes.

## Car wash
Model posts with statuses AVAILABLE, RESERVED, OCCUPIED, CLEANING, MAINTENANCE, OUT_OF_SERVICE and BLOCKED. Booking overlap is rejected in both Android persistence logic and backend API. Wash order lifecycle covers PREBOOKED/BOOKED/CHECKED_IN/IN_SERVICE/QC/PAYMENT_DUE/PAID/CLOSED/CANCELLED/NO_SHOW. Packages contain ordered service lines and optional material/chemical requirements. Price calculation applies branch price + vehicle class + package + configured discount/surcharge rules and freezes the resulting order snapshot.

Technology cards define operations, durations and normative chemical quantities. Actual chemical consumption is posted to inventory with order/post/employee linkage. Variance is norm vs fact by chemical and order; report exposes excess quantity/value. QC is recorded before close when enabled.

## Tire service and storage
Tire order lifecycle mirrors booking, check-in, service, QC, payment and close. Operations accept wheel count 1/2/4 and target wheel positions. A vehicle may have multiple wheel/tire sets. Each wheel inspection stores tread depth, pressure, rim condition, tire manufacturer/model, production date, defect notes and attachment ids. Defect images are normal document/file attachments, not transient UI state.

Storage uses a storage contract, expected end date, extensions as immutable events, managed locations (site/zone/rack/slot), location-movement history, and QR/barcode label identifiers. Pickup/release closes active reminders.

## Loyalty and analytics
Use a transaction ledger for earn/redeem/expire/adjust; account balance is derived/transactionally maintained and cannot become negative. Rules define earn rate/amount, eligible modules/services, tier, date range, cap and exclusions. Redemption enforces min/max share, expiry and exclusions. Customer tier is derived from configured thresholds. Reports show issued/redeemed/expired/outstanding bonuses and customers by tier.

Fault analytics groups deterministic persisted fault/service codes and vehicle/equipment identifiers; repeat-request analytics uses documented identifier/time-window rules. No semantic guessing is used where a stable code/link is absent.

## External and integration platform
Portal APIs are authenticated, tenant-scoped and expose only explicitly whitelisted read/write operations. Online booking API validates organization/branch/module entitlement and uses server-side overlap protection. Queue display API is read-only and purpose-limited. External API is versioned under `/api/v1` with compatibility policy.

Webhooks use an outbox with idempotency key, signed payload, attempt count, next retry, terminal failure and error log. Imports support validation preview, row errors, idempotent apply and audit. Exports use the same domain data as UI. Files support metadata, checksums, versions, signed-document immutability and resumable upload protocol.

## Security
Backend is authoritative for RBAC, organization, branch, object type, operation and ownership constraints. UI hiding is convenience only. Every module-protected backend operation checks effective entitlement. Tokens remain in Android Keystore-backed storage; device-bound sessions and revocation remain enforced. Production transport requires TLS and environment-specific endpoints/config.

## Testing and release
Required suites: unit business rules, repository tests, sync tests, migration tests, API tests, RBAC/tenant-isolation tests, UI smoke tests, and regression. End-to-end acceptance includes: core service workflow; offline→reconnect→sync without duplicates; full car wash; full tire service/storage; client→object→equipment→request→assignee→work→act→payment→close.

Release readiness additionally requires signed release build, update over previous supported APK with data preservation, server migrations and rollback plan, production backup before migration, health check, version/commit/build traceability and smoke verification on the current revision.

## UX and accessibility
Move user-facing strings into resources as touched; preserve Lexora design contract. Minimum touch targets, large-font behavior, light/dark contrast, selection/back behavior and scroll restoration are acceptance concerns. Bulk select/actions must be present for lists where the TЗ requires mass operations.

## Audit
Audit critical operations including price changes, discounts/surcharges, payments/refunds/adjustments, role/right changes, licenses, imports/exports, signed approvals, contract versions and archival/restore. Audit records include actor, organization/branch, entity, before/after or immutable payload, reason where required, result, source and correlation id.