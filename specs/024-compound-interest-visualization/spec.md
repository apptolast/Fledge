# FLE-49 Compound Interest Visualization

## User Story

As a child, I want to see how interest can grow my virtual balance over time so the habit of saving feels concrete and age-appropriate.

## Acceptance Criteria

### AC-01 Show Projection When Useful

Given parent interest is enabled and the child has a positive main balance
When the child opens the home screen
Then the app shows a compound-interest card with today, one-year and three-year values.

### AC-02 Hide When Not Applicable

Given parent interest is disabled or the child main balance is zero
When the child opens the home screen
Then no compound-interest card is shown.

### AC-03 Age-Adapted Explanation

Given the child has a birth year
When the projection is shown
Then the explanation text uses a simpler message for younger children and a compound-interest message for older children.

### AC-04 Stable Calculation

Given a balance and annual interest rate
When the projection is calculated
Then it compounds monthly and rounds down to whole cents.

## Notes

- This is educational only; it does not create ledger transactions.
- It uses the family interest settings from FLE-48.
- The visualization must be aligned in Pencil before Compose implementation.
- Pencil screen: `Screen / Home hijo - interes compuesto` (`GOQIm`), copied from the existing child home screen.
