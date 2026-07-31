# FLE-57 Streaks And Badges

## User Story

As a child, I want to see simple streaks and badges based on the tasks I really complete so I can understand my progress without the app inventing rewards disconnected from family activity.

## Acceptance Criteria

### AC-01 Task Streak Uses Real Approved Tasks

Given a child has task instances approved by a parent
When Fledge calculates the child habit summary
Then it derives the current and best streak from approved task review dates, grouped by the family timezone.

### AC-02 Badges Are Derived From Existing MVP Data

Given a child has approved task activity
When Fledge builds badge progress
Then it exposes lightweight badges for first approved task day, three approved task days and three consecutive approved days without persisting a new badge system.

### AC-03 Child Home Shows A Compact Habit Card

Given the child opens Home
When task history exists
Then the screen shows a compact streak and badge card before the task list, using the same visual language as the existing child dashboard.

### AC-04 Empty Activity Does Not Fake Achievements

Given a child has no approved task history
When the child opens Home
Then Fledge keeps achievements locked/progress-based and does not show earned badges.

## Design

- Pencil: add `Screen / Home hijo - rachas y badges`.
- Placement: between goal/celebration content and `Tus tareas`.
- No separate achievements route in phase 1.

## Notes

- No Fastlane, GitHub Actions or FLE-26 work in this sprint.
- FLE-94 remains the future task for animated goal completion celebrations.

## Traceability

- AC-01 -> `ChildAchievementCalculatorTest`.`FLE-57 AC-01 derives current and best streak from approved task review days`
- AC-02 -> `ChildAchievementCalculatorTest`.`FLE-57 AC-02 exposes badge progress from existing approved tasks`
- AC-03 -> `ChildHomeViewModelTest`.`FLE-57 AC-03 child home exposes achievement summary before task list`
- AC-04 -> `ChildAchievementCalculatorTest`.`FLE-57 AC-04 empty activity does not fake earned achievements`
