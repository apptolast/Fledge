# FLE-50 Spend Save Give Pots

## User Story

As a parent and child, I want virtual money split into Spend, Save and Give pots so simple financial planning is visible inside Fledge.

## Acceptance Criteria

### AC-01 Pot Model

Given the app has existing MAIN and GOAL ledger data
When pot support is enabled
Then Spend maps to MAIN, Save maps to GOAL and Give maps to a new GIVE virtual account without breaking older goals.

### AC-02 Goal Pot Selection

Given a parent creates a new goal
When they choose Save or Give
Then the saved goal stores the selected pot type and uses the matching virtual account.

### AC-03 Pot Balances On Child Home

Given a child has balances in MAIN, GOAL and GIVE
When the child opens Home
Then the screen shows Spend, Save and Give balances as separate pots.

### AC-04 Goal Transfers Use Goal Pot

Given a Save or Give goal
When the child deposits or withdraws money
Then the transfer moves money between MAIN and the goal account associated with that pot.

### AC-05 Firestore Compatibility

Given older savings goals without a `potType`
When they are read from Firestore
Then they default to Save.

Given a parent writes a Save or Give goal
When Firestore rules validate it
Then the write is allowed only when `potType` and `accountType` match.

## Notes

- Spend is the existing MAIN balance and is not created as a savings goal in this phase.
- Pencil screens: `Screen / Home hijo - botes` (`c5ASor`) and `Screen / Nuevo objetivo - botes` (`UfPts`), both copied from existing screens and validated without layout problems.
