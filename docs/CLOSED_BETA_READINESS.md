# Closed Beta Readiness

FLE-45 prepares a closed beta with 10-15 families. The goal is to decide whether the MVP loop works
in real family use without collecting child-level behavioural analytics.

## Cohort

- Target: 10-15 active families.
- Include families that have at least one parent account, one child profile and one completed core loop.
- Do not expand beyond 15 families until the exit criteria are green; above 15 is no longer a closed
  beta signal.

## Aggregate Metrics

Only aggregated metrics are allowed in the readiness report:

- `activeFamilyCount`
- task review durations, aggregated as mean submitted-to-reviewed time
- cash-out durations, aggregated as mean requested-to-confirmed time
- count of open cash-outs older than 72 h
- count of balance discrepancies
- observation period in days

Do not include family ids, child ids, device ids, names, emails, task titles, concepts or individual
ledger rows in the readiness report.

## Exit Criteria

The beta is `Ready` only when all gates pass:

- Cohort has 10-15 active families.
- Mean task approval time is at most 24 h.
- Balance discrepancy count is 0 after at least 28 days of observation.
- Mean cash-out time is at most 72 h.
- No open cash-out is older than 72 h.

If any operational threshold fails, the beta is `AtRisk`.
If the cohort, event sample or 28-day balance window is incomplete, the beta is `NeedsMoreData`.

## Review Cadence

- Review aggregates weekly during the closed beta.
- Record the readiness status and the blocking metric.
- Do not use screenshots or exports containing child/family identifiers in the readiness note.
- Keep qualitative feedback separate from analytics and redact names before sharing.

## Source Of Truth

The code contract lives in:

- `ClosedBetaMetricSnapshot`
- `ClosedBetaReadinessThresholds`
- `ClosedBetaReadinessEvaluator`

Use the evaluator output as the release-readiness decision input for FLE-45.
