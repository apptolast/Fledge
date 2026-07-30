# FLE-48 Parent Interest

## User Story

As a parent, I want to configure a virtual interest rate for the family so children can see regular saving growth without Fledge moving real money.

## Acceptance Criteria

### AC-01 Configure Interest

Given a signed-in parent with a family
When they enable parent interest and enter a valid annual rate and posting day
Then the family document stores the settings without changing locked currency or time zone fields.

### AC-02 Reject Invalid Settings

Given a signed-in parent with a family
When they enable parent interest with a zero or unsupported annual rate, or a posting day outside 1-28
Then the app shows a validation error and does not persist the change.

### AC-03 Scheduled Accrual

Given a family with parent interest enabled and child main-account balances
When the scheduled job runs on the configured local posting day
Then it creates one `Interest` ledger transaction per child with a positive calculated amount.

### AC-04 Idempotent Periods

Given interest has already been posted for a child and period
When the scheduled job runs again for the same period
Then it does not create duplicate interest transactions.

## Notes

- Interest is virtual money only and uses ledger transactions with `createdBy = System`.
- Interest is calculated from positive `Main` account balances only.
- The monthly amount is `floor(balanceCents * annualRateBasisPoints / 10000 / 12)`.
