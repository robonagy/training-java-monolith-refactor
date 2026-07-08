# Speckit Plan: Legacy Monolith Modernization (Phase 3b Loops)

Date: 2026-07-08
Planning mode: Incremental strangler replacement with behavior lock-in first

## Plan Objective

Execute module-by-module replacement of legacy monolith behavior while preserving externally visible behavior defined by rediscovery evidence.

Primary source of truth:
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)

## Constraints and Guardrails

- No behavior invention: when behavior is ambiguous, stop and request a product decision before implementing.
- Preserve legacy contracts at module boundaries until cutover is approved.
- Require behavior-preserving tests before and after each module switch.
- Keep changes reversible via feature toggle/adapter boundary per module.

## Current Baseline and Readiness

- Available baseline artifacts:
  - [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
  - [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)
  - [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
  - [SPECKIT_TASKS_PHASE3B.md](SPECKIT_TASKS_PHASE3B.md)
- Current implementation baseline:
  - Billing compatibility boundary introduced in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingContract.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingContract.java)
  - Delegating module adapter introduced in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/LegacyCompatibleBillingModule.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/LegacyCompatibleBillingModule.java)
  - Billing contract tests added in [src/test/java/com/sourcegraph/demo/bigbadmonolith/service/BillingServiceContractTest.java](src/test/java/com/sourcegraph/demo/bigbadmonolith/service/BillingServiceContractTest.java)
  - Dual-run diffing capability added in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingDualRunComparator.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingDualRunComparator.java)
  - Dual-run report model added in [src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingComparisonReport.java](src/main/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingComparisonReport.java)
  - Dual-run tests added in [src/test/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingDualRunComparatorTest.java](src/test/java/com/sourcegraph/demo/bigbadmonolith/service/billing/BillingDualRunComparatorTest.java)
- Environment state:
  - Gradle tests pass in-session when JAVA_HOME is set to Java 21.
  - Machine-level JAVA_HOME persistence is still pending.

Readiness gate R0:
- JAVA_HOME points to a valid Java 21 installation.
- Command succeeds: ./gradlew test

Current R0 assessment:
- Partially satisfied (session-level pass confirmed; machine-level persistence pending).

## Execution Status Snapshot

- Completed:
  - Phase-1 rediscovery baseline captured.
  - Phase-2 substitution audit completed.
  - Phase-3 target architecture specification completed.
  - Billing contract baseline implementation completed and verified by tests.
  - Billing dual-run comparison path completed and verified by tests.
- In progress:
  - Environment stabilization finalization (persist JAVA_HOME).
- Pending high-priority planning artifacts:
  - MODULE_REWRITE_PHASE3B_BILLING.md

## Work Breakdown Structure

## Phase 0: Environment Stabilization

Goal:
- Restore reliable local verification loop.

Tasks:
1. Fix JAVA_HOME to valid Java 21 path.
2. Verify Gradle wrapper test run end-to-end.
3. Capture known run commands and expected outputs in a repo note.

Exit criteria:
- R0 passed.
- Test command reproducible on this workspace.

## Phase 1: Artifact Stabilization (Planning Inputs)

Goal:
- Confirm complete planning artifacts required for loop execution.

Tasks:
1. Validate substitution audit remains aligned with rediscovery evidence.
2. Validate target architecture artifact remains aligned with rediscovery + substitution audit evidence.
3. Produce loop-1 billing module rewrite spec with FR -> AT -> BR trace matrix.

Deliverables:
- SUBSTITUTION_AUDIT_PHASE1.md
- TARGET_ARCHITECTURE_PHASE3.md
- MODULE_REWRITE_PHASE3B_BILLING.md

Exit criteria:
- Every planned module requirement traces back to rediscovery rule IDs.
- All artifacts present in repository root and cross-linked.

## Phase 2: Cross-Cutting Test Harness (Before First Cutover)

Goal:
- Build black-box contract tests that detect behavior drift.

Tasks:
1. Define test fixtures for customer/category/hour combinations that exercise BR-01 through BR-06 first.
2. Add baseline contract tests at BillingService-equivalent boundary.
3. Add deterministic comparison outputs for map keys, string messages, and decimal totals.
4. Add safety tests for known legacy quirks that must remain stable until intentionally changed.

Trace focus:
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L8)
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L19)
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L52)

Exit criteria:
- Contract suite fails on behavior drift and passes on legacy baseline.

## Phase 3: Loop 1 Implementation (Billing Module)

Goal:
- Introduce rewritten billing module behind legacy-identical interface.

Tasks:
1. Implement billing module adapter that preserves legacy method signatures and payload key shapes.
2. Run legacy and rewritten module in parallel in non-prod for diff comparison.
3. Gate cutover on full parity across billing acceptance tests.
4. Switch traffic to rewritten billing module with rollback switch retained.

Entry criteria:
- Phase 2 contract harness complete.
- Billing loop spec approved.

Exit criteria:
- Billing contract parity achieved for all in-scope cases.
- No unresolved billing ambiguities remain.

## Phase 4: Repeated Module Loops (Reporting, Hours, Category, User, Customer)

Goal:
- Continue strangler loop for each remaining module boundary.

Per-module loop template:
1. Produce module rewrite spec with FR -> AT -> BR mapping.
2. Add/extend contract tests from rediscovery rules.
3. Implement module behind legacy-compatible boundary.
4. Execute dual-run diff and resolve mismatches.
5. Cut over with rollback path retained.

Recommended sequence:
1. Reporting
2. Time Entry (hours)
3. Category/Pricing
4. User
5. Customer

Reason for ordering:
- Highest business-risk behavior (billing/reporting correctness) is stabilized first.
- CRUD-focused modules follow after financial/reporting behavior is controlled.

Exit criteria:
- Each module reaches tested contract parity before next module cutover.

## Phase 5: Final Convergence and Legacy Path Retirement

Goal:
- Remove obsolete legacy execution paths after full parity.

Tasks:
1. Remove obsolete adapters and dead code paths module-by-module.
2. Keep regression contract suite and smoke tests green.
3. Update operational docs and runbooks for new boundaries.
4. Final production-readiness review.

Exit criteria:
- No runtime traffic depends on retired legacy module implementations.
- Full regression suite and smoke suite pass.

## Decision Gates (Ambiguity Stops)

Implementation must pause and request business decision when encountering unresolved questions from rediscovery, including:

1. Reporting authority when service and JSP SQL differ.
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299)

2. Weekend logging policy (warning-only vs rejection).
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L304)

3. Monthly range behavior (fixed day-31 vs true month end).
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L313)

4. users.jsp name/email swap behavior (intentional vs defect).
- [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L317)

No module cutover proceeds through these behaviors without explicit decision records.

## Risk Register (Execution)

1. Environment risk: local tests blocked by JAVA_HOME misconfiguration.
2. Financial accuracy risk: mixed decimal/double behavior in legacy surface can cause parity drift.
3. Hidden coupling risk: JSP pages contain direct JDBC/business logic not represented in service APIs.
4. Data integrity risk: delete and FK behavior can break downstream module assumptions.

Mitigation pattern:
- Contract tests first, dual-run comparisons, reversible cutovers, explicit decision logs.

## Milestone Summary

1. M0: Environment ready and tests runnable. Status: in progress (session pass complete, machine persistence pending).
2. M1: Planning artifacts restored and aligned. Status: in progress (substitution audit and target architecture complete; billing rewrite spec pending).
3. M2: Billing contract harness complete. Status: complete.
4. M3: Billing module cutover complete with rollback path. Status: in progress (dual-run comparator complete; parity-resolution and switch gate pending).
5. M4: Remaining module loops completed in sequence. Status: pending.
6. M5: Legacy paths retired and modernization baseline established. Status: pending.

## Immediate Next Steps

1. Persist machine-level JAVA_HOME to Java 21 and reconfirm ./gradlew test without session overrides.
2. Create [MODULE_REWRITE_PHASE3B_BILLING.md](MODULE_REWRITE_PHASE3B_BILLING.md) with FR -> AT -> BR traceability.
3. Run parity-resolution workflow using dual-run outputs for billing acceptance scenarios.
4. Prepare billing cutover gate criteria and rollback-switch validation evidence.
