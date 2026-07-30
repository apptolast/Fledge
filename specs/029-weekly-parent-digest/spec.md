# FLE-54 Weekly Parent Digest

## User Story

As a parent, I want a weekly in-app summary of the family routine so I can quickly re-engage with tasks, savings goals, cash-outs and pending debt.

## Acceptance Criteria

### AC-01 Weekly Digest Summarizes Family Activity

Given a parent has an active family with child profiles
When they open the weekly summary
Then Fledge shows the last 7 days of submitted, approved and rejected tasks, savings contributions, goal withdrawals and requested cash-outs.

### AC-02 Pending Parent Actions Stay Visible

Given a submitted task or unconfirmed cash-out remains open
When the weekly summary is calculated
Then Fledge includes it in the pending parent actions even if the original request was created before the last 7 days.

### AC-03 Child-Level Highlights Are Shown

Given the family has one or more child profiles
When the parent reviews the digest
Then Fledge shows per-child task, savings, cash-out and active-goal highlights.

### AC-04 Empty Week Encourages Reactivation

Given there is no weekly activity and no pending action
When the parent opens the digest
Then Fledge shows a low-activity state that nudges the parent to create a task or review goals.

### AC-05 Parent Home Links To Digest

Given the parent home has at least one child profile
When the dashboard is rendered
Then Fledge exposes a weekly summary action that navigates to the digest.

## Notes

- Phase 1 is in-app only; push/email digests are out of scope.
- No new backend writes or Firestore rules are required for this phase.
- The digest uses existing task, ledger, savings goal and cash-out repositories.
