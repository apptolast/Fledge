# Fledge Functions

Cloud Functions Gen2 for Fledge money flows.

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
