# FLE-51 Parent Match

## User Story

As a parent, I want to configure a virtual match on child goal contributions so saving or giving can be reinforced without moving real money inside Fledge.

## Acceptance Criteria

### AC-01 Configure Match

Given a parent has a family
When they enable parent match and enter a valid match percentage and cap per contribution
Then the family stores the match settings without unlocking currency or timezone settings.

### AC-02 Validate Match Settings

Given parent match is enabled
When the percentage is zero, above 100%, or the cap is not positive
Then the settings are rejected and the previous match configuration remains unchanged.

### AC-03 Apply Match On Goal Deposit

Given parent match is enabled for the family
When the child deposits money into a Save or Give goal
Then Fledge writes the normal MAIN -> goal transfer and adds a `Match` ledger transaction to the same goal account.

### AC-04 Match Cap

Given parent match has a cap per contribution
When the calculated match exceeds that cap
Then the `Match` ledger transaction uses the capped amount.

### AC-05 Disabled Match

Given parent match is disabled
When the child deposits into a goal
Then no extra match transaction is created.

## Notes

- Match is virtual money only and uses `LedgerTransactionType.Match` with `createdBy = System`.
- Match applies only to child-created goal deposits.
- Pencil screen: `Screen / Match parental` (`ICvDC`), validated without layout problems.
