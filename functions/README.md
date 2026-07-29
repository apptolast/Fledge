# Fledge Functions

Cloud Functions Gen2 for Fledge money and task flows.

## Allowance scheduler

`runAllowanceRules` runs hourly in `europe-west1`. It reads due rules from:

```text
families/{familyId}/allowanceRules/{allowanceRuleId}
```

Expected rule fields:

- `familyId`: string
- `childProfileId`: string
- `accountType`: `Main` or `Goal`
- `frequency`: `Weekly` or `Monthly`
- `day`: weekly ISO day `1..7` or monthly day `1..31`
- `amountCents`: positive integer
- `concept`: non-empty string
- `timeZone`: IANA time zone, defaults to `Europe/Madrid`
- `nextRunAt`: Firestore timestamp
- `active`: boolean

For each due rule it creates an idempotent ledger document:

```text
families/{familyId}/ledgerTransactions/allowance_{allowanceRuleId}_{yyyyMMdd}
```

Monthly rules configured for day `31` run on the last day of shorter months.

## Task assignment scheduler

`runTaskAssignments` runs hourly in `europe-west1`. It reads due assignments from:

```text
families/{familyId}/taskAssignments/{taskAssignmentId}
```

Expected assignment fields:

- `familyId`: string
- `taskTemplateId`: string
- `title`: non-empty string
- `rewardCents`: positive integer
- `requiresPhoto`: boolean
- `childProfileIds`: non-empty string array
- `recurrence`: `Once`, `Daily`, `Weekly` or `Custom`
- `customIntervalDays`: positive integer only for `Custom`
- `dueAt`: Firestore timestamp
- `active`: boolean

For each due assignment it creates one idempotent pending instance per child:

```text
families/{familyId}/taskInstances/task_{taskAssignmentId}_{childProfileId}_{yyyyMMdd}
```

The `yyyyMMdd` period key is calculated from `dueAt` in the family time zone,
falling back to `Europe/Madrid`.

After processing:

- `Once` assignments are marked inactive.
- `Daily`, `Weekly` and `Custom` assignments advance `dueAt`.
- No ledger money is created here. Task rewards are paid only when later approved.

## Task approval push

`notifyTaskInstancePush` reacts to writes in:

```text
families/{familyId}/taskInstances/{taskInstanceId}
```

It sends one topic-based FCM notification when the task status changes:

- `Submitted`: parent topic `fledge_<env>_family_<familyId>_parents`, payload type
  `task_submitted`.
- `Approved`: child topic `fledge_<env>_family_<familyId>_child_<childProfileId>`, payload type
  `task_approved`.

The debug database uses the `Debug` function variant and `fledge_debug...` topics. The default
database uses the release function variant and `fledge_release...` topics.

## Approval queue reminders

`runApprovalQueueReminders` runs every 6 hours in `europe-west1`. It scans submitted task
instances and sends a single parent reminder per family when either:

- More than 5 submitted tasks are waiting for approval.
- Or the oldest submitted task has been waiting for at least 72 hours.

The reminder payload type is `approval_queue_reminder` and navigates to the parent approval queue.
The family document stores `approvalQueueReminderLastSentAt` so reminders are not repeated within
24 hours.
