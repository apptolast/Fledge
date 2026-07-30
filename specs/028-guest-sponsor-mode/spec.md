# FLE-53 Guest Sponsor Mode

## User Story

As a family owner, I want to invite a grandparent or trusted guest to sponsor a child so they can contribute gifts or matching money without becoming a family admin.

## Acceptance Criteria

### AC-01 Owner Invites Guest For Child

Given the signed-in owner has a family with child profiles
When they invite a guest email for a child
Then Fledge stores a normalized active guest invitation scoped to that child.

### AC-02 Guest Login Routes To Guest Home

Given a user signs in with an active guest invitation email and does not own a family
When post-login routing resolves the account
Then Fledge navigates to the guest home instead of family setup.

### AC-03 Guest Can Contribute Gift Or Match

Given a guest has an active invitation for a child
When they submit a positive contribution amount and concept
Then Fledge creates a positive ledger transaction for that sponsored child with actor `Guest`.

### AC-04 Guest Cannot Manage Family Data

Given a guest has an active invitation
When they try to update family root, child profiles, tasks, allowances or settlements
Then Firestore rules reject those writes.

### AC-05 Guest Scope Is Child-Limited

Given a guest sponsors one child
When they read child or ledger documents
Then Firestore rules allow only documents for sponsored children.

## Notes

- FLE-53 phase 1 does not send email; it stores the guest access invite.
- Guest contribution settlement with real payments remains out of scope.
- The guest path is separate from secondary admins; guests never become parents/admins.
