# Speckit Tasks: Phase 3b Modernization Execution

Date: 2026-07-08
Source plan: [SPECKIT_PLAN_PHASE3B.md](SPECKIT_PLAN_PHASE3B.md)
Primary behavior baseline: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)

## Task Status Legend

- TODO: not started
- IN_PROGRESS: currently being worked
- BLOCKED: waiting on dependency or business decision
- DONE: completed and verified

## Task Board

| ID | Status | Task | Depends On | Verification / Done Criteria |
|---|---|---|---|---|
| T-0001 | IN_PROGRESS | Fix JAVA_HOME to valid Java 21 installation path on workspace machine | None | Session-level JAVA_HOME confirmed and stable machine-level configuration pending |
| T-0002 | DONE | Re-run baseline tests with Gradle wrapper | T-0001 | .\\gradlew.bat test exits 0 |
| T-0003 | TODO | Record local runbook commands for this workspace (build/test/liberty start) | T-0002 | Commands documented and reproducible locally |
| T-0004 | DONE | Recreate phase-2 substitution audit artifact from rediscovery evidence | T-0002 | SUBSTITUTION_AUDIT_PHASE1.md exists and maps substitutions to rediscovery behavior |
| T-0005 | TODO | Recreate phase-3 target architecture artifact with explicit module boundaries | T-0004 | TARGET_ARCHITECTURE_PHASE3.md exists and includes boundary definitions + trace matrix |
| T-0006 | TODO | Confirm loop-1 billing spec is current and aligned with latest baseline | T-0005 | MODULE_REWRITE_PHASE3B_BILLING.md exists and includes FR -> AT -> BR mapping |
| T-0007 | DONE | Build billing contract test fixtures (customer/category/hour scenarios) | T-0006 | Fixture set covers BR-01..BR-06 behavior variants |
| T-0008 | DONE | Add billing boundary contract tests for maps/messages/decimal totals | T-0007 | Tests fail on intentional behavior drift and pass on baseline |
| T-0009 | DONE | Add regression tests for known legacy quirks in billing scope | T-0008 | Quirk tests codified with explicit expected outputs |
| T-0010 | DONE | Implement rewritten billing module behind legacy-identical interface | T-0009 | Public signatures and payload key shapes match legacy contract |
| T-0011 | TODO | Add dual-run comparison path for legacy vs rewritten billing in non-prod | T-0010 | Diff output available for all billing acceptance scenarios |
| T-0012 | TODO | Resolve any billing parity mismatches found in dual-run | T-0011 | No mismatches remain for billing acceptance suite |
| T-0013 | TODO | Cut over traffic to rewritten billing module with rollback switch retained | T-0012 | Billing path switched; rollback mechanism validated |
| T-0014 | TODO | Create reporting module rewrite spec (FR -> AT -> BR) | T-0013 | MODULE_REWRITE_PHASE3B_REPORTING.md exists and traceable |
| T-0015 | BLOCKED | Resolve reporting authority decision (service logic vs reports.jsp SQL) | T-0014 | Decision record approved and linked to task output |
| T-0016 | TODO | Implement reporting module behind legacy-compatible boundary | T-0015 | Reporting contract parity achieved in tests |
| T-0017 | TODO | Create time-entry module rewrite spec (FR -> AT -> BR) | T-0016 | MODULE_REWRITE_PHASE3B_TIME_ENTRY.md exists and traceable |
| T-0018 | BLOCKED | Resolve weekend policy decision (warning-only vs rejection) | T-0017 | Decision record approved and linked to task output |
| T-0019 | TODO | Implement time-entry module + parity tests | T-0018 | Time-entry behavior parity passed |
| T-0020 | TODO | Create category/pricing module rewrite spec (FR -> AT -> BR) | T-0019 | MODULE_REWRITE_PHASE3B_CATEGORY_PRICING.md exists and traceable |
| T-0021 | TODO | Implement category/pricing module + parity tests | T-0020 | Category/pricing behavior parity passed |
| T-0022 | TODO | Create user module rewrite spec (FR -> AT -> BR) | T-0021 | MODULE_REWRITE_PHASE3B_USER.md exists and traceable |
| T-0023 | BLOCKED | Resolve user name/email swap intent decision | T-0022 | Decision record approved and linked to task output |
| T-0024 | TODO | Implement user module + parity tests | T-0023 | User behavior parity passed |
| T-0025 | TODO | Create customer module rewrite spec (FR -> AT -> BR) | T-0024 | MODULE_REWRITE_PHASE3B_CUSTOMER.md exists and traceable |
| T-0026 | TODO | Resolve delete-policy details where FK constraints affect behavior | T-0025 | Decision/behavior confirmed and encoded in tests |
| T-0027 | TODO | Implement customer module + parity tests | T-0026 | Customer behavior parity passed |
| T-0028 | TODO | Remove obsolete adapters and dead legacy paths after full parity | T-0016, T-0019, T-0021, T-0024, T-0027 | No production traffic depends on retired paths |
| T-0029 | TODO | Run full regression suite and smoke checks before final signoff | T-0028 | All tests pass; no blocker defects |
| T-0030 | TODO | Publish final modernization runbook and boundary documentation | T-0029 | Docs updated and reviewed |

## Ordered Execution Buckets

1. Bucket A - Environment readiness
- T-0001
- T-0002
- T-0003

2. Bucket B - Planning inputs stabilization
- T-0004
- T-0005
- T-0006

3. Bucket C - Billing loop (first cutover)
- T-0007
- T-0008
- T-0009
- T-0010
- T-0011
- T-0012
- T-0013

4. Bucket D - Remaining module loops
- Reporting: T-0014, T-0015, T-0016
- Time Entry: T-0017, T-0018, T-0019
- Category/Pricing: T-0020, T-0021
- User: T-0022, T-0023, T-0024
- Customer: T-0025, T-0026, T-0027

5. Bucket E - Convergence and retirement
- T-0028
- T-0029
- T-0030

## Decision Backlog (Must Be Resolved Before Affected Cutovers)

| Decision ID | Blocking Task(s) | Question | Evidence |
|---|---|---|---|
| D-01 | T-0015 | Which reporting behavior is authoritative when service and JSP SQL differ? | [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L299) |
| D-02 | T-0018 | Is weekend logging warning-only or hard rejection? | [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L304) |
| D-03 | Reporting implementation scope | Should monthly reporting keep fixed day-31 behavior or use true month-end? | [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L313) |
| D-04 | T-0023 | Is users.jsp name/email constructor order intentional or defect? | [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md#L317) |

## Immediate Next 5 Tasks

1. T-0001 Finalize machine-level JAVA_HOME to Java 21.
2. T-0003 Record local runbook commands for this workspace.
3. T-0005 Recreate phase-3 target architecture artifact with explicit module boundaries.
4. T-0006 Confirm loop-1 billing spec is current and aligned with latest baseline.
5. T-0011 Add dual-run comparison path for legacy vs rewritten billing in non-prod.
