# FLE-55 Statement Export

## User Story

As a parent, I want family and child statements in CSV/PDF format so I can audit movements with a bank-like level of trust.

## Acceptance Criteria

### AC-01 Family Statement Exports Ledger And Cash-Out Rows

Given a parent has an active family with ledger transactions and cash-out requests
When they generate a family statement
Then Fledge includes all family movements in a CSV artifact with stable columns and escaped dynamic text.

### AC-02 Child Statement Filters By Child

Given the family has more than one child profile
When the parent selects a child scope
Then Fledge includes only that child's ledger and cash-out movements.

### AC-03 PDF Artifact Is Generated

Given the parent selects PDF format
When the statement is generated
Then Fledge creates an application/pdf artifact with a readable statement preview.

### AC-04 Parent Home Links To Statements

Given the parent home has at least one child profile
When the dashboard is rendered
Then Fledge exposes a statement export action that navigates to the export screen.

### AC-05 Empty State Handles Missing Family

Given there is no active family
When the statement export screen is opened
Then Fledge shows an empty state instead of generating an invalid artifact.

## Notes

- Phase 1 is in-app generation and preview of CSV/PDF artifacts.
- Native share/save sheets are out of scope for this task.
- No new backend writes, functions or Firestore rules are required.
