# FLE-52 Secondary Admin

## User Story

As the family owner, I want to invite a second parent/admin by email so another trusted adult can help run day-to-day family routines without owning sensitive family settings.

## Acceptance Criteria

### AC-01 Owner Invites Admin

Given the signed-in owner has an active family
When they enter a valid email and send an admin invite
Then Fledge stores the normalized email as an active admin invitation for that family.

### AC-02 Invitation Grants Family Access

Given an invited admin signs in with the invited email
When Fledge resolves the active family
Then the admin is bound to the owner's family instead of being forced through family setup.

### AC-03 Admin Has Limited Permissions

Given an invited admin has access to the family
When they operate family subcollections such as children, tasks, goals, allowances, ledger and devices
Then the Firestore rules allow day-to-day parent operations.

### AC-04 Admin Cannot Manage Sensitive Family Root

Given an invited admin has access to the family
When they try to change owner-only family root settings or invite another admin
Then the Firestore rules reject the write.

### AC-05 Owner Can Revoke Invite

Given the owner has an active admin invite
When they revoke it
Then the admin email is removed from family access and the invite is marked revoked.

## Notes

- Existing families keep backward compatibility: `familyId == owner uid` still means owner.
- FLE-52 does not add email delivery; it stores the access invite so the invited user can sign in with that email.
- Pencil screen: `Screen / Co-padre` (`V46dfP`), validated without layout problems.
