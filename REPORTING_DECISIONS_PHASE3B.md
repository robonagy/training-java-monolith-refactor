# Reporting Ambiguity Decisions (Phase 3b)

Date: 2026-07-08
Status: Approved inputs for Reporting module rewrite loop

## Decision D-REP-01: Authoritative behavior source
- Decision: Service-layer billing logic is authoritative.
- Source question: [Open Question 1](REDISCOVERY_SPEC.md#L262)
- Effect:
  - Where service behavior exists, it is canonical.
  - Legacy reports.jsp SQL path is treated as compatibility surface, not semantic authority.

## Decision D-REP-02: Monthly boundary rule
- Decision: Calendar month end is authoritative.
- Source question: [Open Question 4](REDISCOVERY_SPEC.md#L274)
- Effect:
  - Monthly period upper bound uses true last day of month.
  - Legacy fixed day-31 behavior is not carried forward as normative module policy.

## Trace
- Rediscovery source: [REDISCOVERY_SPEC.md](REDISCOVERY_SPEC.md)
- Architecture source: [TARGET_ARCHITECTURE_PHASE3.md](TARGET_ARCHITECTURE_PHASE3.md)
- Substitution source: [SUBSTITUTION_AUDIT_PHASE1.md](SUBSTITUTION_AUDIT_PHASE1.md)
