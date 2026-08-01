# FLE-96 Child Profile Entry

## User Story

As a parent and child using a shared family device, I want each child screen to use the real child profile and the parent dashboard to open a child's profile directly so the navigation matches the family currently configured.

## Acceptance Criteria

### AC-01 Child Home Greets The Active Child

Given a child profile named Elisa exists
When the child opens Home for Elisa
Then the title greets Elisa and never uses a hardcoded demo child name.

### AC-02 Parent Home Opens A Child Profile

Given a parent is viewing Home with at least one child card
When the parent taps the child profile action for a child
Then Fledge navigates to that child's Home/profile route.

## Design

- Pencil: update `Screen / Home padre` child card with a direct `Ver perfil` action.
- Existing `Screen / Home hijo - rachas y badges` already shows a dynamic child name in design.

## Notes

- No backend schema changes.
- No Fastlane, GitHub Actions or FLE-26 work.

## Traceability

- AC-01 -> `ChildHomeViewModelTest`.`FLE-96 AC-01 child home greets the active child profile`
- AC-02 -> `ParentHomeNavigationTest`.`FLE-96 AC-02 parent home route can navigate to a child home route`
