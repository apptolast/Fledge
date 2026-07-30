import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { DateTime } from "luxon";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const DEFAULT_TIME_ZONE = "Europe/Madrid";
const MAX_RULES_PER_RUN = 100;
const MAX_TASK_ASSIGNMENTS_PER_RUN = 100;
const MAX_INTEREST_FAMILIES_PER_RUN = 100;

export const runAllowanceRules = makeRunAllowanceRules(getFirestore(), "runAllowanceRules");
export const runAllowanceRulesDebug = makeRunAllowanceRules(getFirestore("debug"), "runAllowanceRulesDebug");
export const runTaskAssignments = makeRunTaskAssignments(getFirestore(), "runTaskAssignments");
export const runTaskAssignmentsDebug = makeRunTaskAssignments(getFirestore("debug"), "runTaskAssignmentsDebug");
export const runInterestAccruals = makeRunInterestAccruals(getFirestore(), "runInterestAccruals");
export const runInterestAccrualsDebug = makeRunInterestAccruals(getFirestore("debug"), "runInterestAccrualsDebug");
export const processAccountDeletionRequests = makeProcessAccountDeletionRequests(
  getFirestore(),
  getAuth(),
  "processAccountDeletionRequests",
);
export const processAccountDeletionRequestsDebug = makeProcessAccountDeletionRequests(
  getFirestore("debug"),
  getAuth(),
  "processAccountDeletionRequestsDebug",
  "debug",
);

function makeRunAllowanceRules(database, functionName) {
  return onSchedule(
    {
      schedule: "every 1 hours",
      timeZone: DEFAULT_TIME_ZONE,
    },
    async () => {
      const processed = await processDueAllowanceRules(database, new Date());
      logger.info(`${functionName} completed`, { processed });
    },
  );
}

function makeRunTaskAssignments(database, functionName) {
  return onSchedule(
    {
      schedule: "every 1 hours",
      timeZone: DEFAULT_TIME_ZONE,
    },
    async () => {
      const processed = await processDueTaskAssignments(database, new Date());
      logger.info(`${functionName} completed`, { processed });
    },
  );
}

function makeRunInterestAccruals(database, functionName) {
  return onSchedule(
    {
      schedule: "every 24 hours",
      timeZone: DEFAULT_TIME_ZONE,
    },
    async () => {
      const posted = await processDueInterestAccruals(database, new Date());
      logger.info(`${functionName} completed`, { posted });
    },
  );
}

function makeProcessAccountDeletionRequests(database, auth, functionName, databaseId = null) {
  const trigger = databaseId
    ? { document: "families/{familyId}", database: databaseId }
    : "families/{familyId}";
  return onDocumentUpdated(trigger, async (event) => {
    const result = await processAccountDeletionRequest({
      database,
      auth,
      familyId: event.params.familyId,
      familyRef: event.data.after.ref,
      before: event.data.before.data(),
      after: event.data.after.data(),
      nowDate: new Date(),
    });
    logger.info(`${functionName} completed`, { familyId: event.params.familyId, ...result });
  });
}

export async function processAccountDeletionRequest({
  database,
  auth,
  familyId,
  familyRef,
  before,
  after,
  nowDate,
}) {
  if (!shouldProcessAccountDeletion(before, after)) {
    return { status: "skipped" };
  }

  const now = Timestamp.fromDate(nowDate);
  try {
    await familyRef.update({
      accountDeletionStatus: "Deleting",
      accountDeletionUpdatedAt: now,
      accountDeletionFailureReason: null,
    });
    const authDeleteStatus = await deleteAuthUser(auth, familyId);
    await database.recursiveDelete(familyRef);
    await writeAccountDeletionAudit(database, familyId, {
      status: "Completed",
      requestedAt: after.accountDeletionRequestedAt ?? null,
      completedAt: now,
      authDeleteStatus,
    });
    return { status: "completed", authDeleteStatus };
  } catch (error) {
    const reason = errorReason(error);
    logger.error("processAccountDeletionRequest failed", { familyId, reason });
    await markAccountDeletionFailed(database, familyRef, familyId, after, now, reason);
    return { status: "failed", reason };
  }
}

function shouldProcessAccountDeletion(before, after) {
  return after?.accountDeletionStatus === "Requested" && before?.accountDeletionStatus !== "Requested";
}

async function deleteAuthUser(auth, uid) {
  try {
    await auth.deleteUser(uid);
    return "Deleted";
  } catch (error) {
    if (isAuthUserNotFound(error)) return "AlreadyDeleted";
    throw error;
  }
}

function isAuthUserNotFound(error) {
  const code = error?.code ?? error?.errorInfo?.code;
  return code === "auth/user-not-found" || String(error?.message).includes("auth/user-not-found");
}

async function markAccountDeletionFailed(database, familyRef, familyId, after, failedAt, reason) {
  await familyRef.update({
    accountDeletionStatus: "Failed",
    accountDeletionUpdatedAt: failedAt,
    accountDeletionFailureReason: reason,
  }).catch((error) => {
    logger.warn("Could not mark account deletion as failed", { familyId, error: errorReason(error) });
  });
  await writeAccountDeletionAudit(database, familyId, {
    status: "Failed",
    requestedAt: after?.accountDeletionRequestedAt ?? null,
    failedAt,
    failureReason: reason,
  });
}

async function writeAccountDeletionAudit(database, familyId, data) {
  await database.collection("accountDeletionAudit").doc(familyId).set({
    familyId,
    updatedAt: data.completedAt ?? data.failedAt,
    ...data,
  });
}

function errorReason(error) {
  return error?.message ? String(error.message) : String(error);
}

export async function processDueAllowanceRules(database, nowDate) {
  const now = Timestamp.fromDate(nowDate);
  const snapshot = await database
    .collectionGroup("allowanceRules")
    .where("active", "==", true)
    .where("nextRunAt", "<=", now)
    .limit(MAX_RULES_PER_RUN)
    .get();

  let processed = 0;
  for (const ruleSnapshot of snapshot.docs) {
    const didProcess = await processRule(database, ruleSnapshot.ref, nowDate);
    if (didProcess) processed += 1;
  }
  return processed;
}

export async function processDueTaskAssignments(database, nowDate) {
  const now = Timestamp.fromDate(nowDate);
  const snapshot = await database
    .collectionGroup("taskAssignments")
    .where("active", "==", true)
    .where("dueAt", "<=", now)
    .limit(MAX_TASK_ASSIGNMENTS_PER_RUN)
    .get();

  let processed = 0;
  for (const assignmentSnapshot of snapshot.docs) {
    const didProcess = await processTaskAssignment(database, assignmentSnapshot.ref, nowDate);
    if (didProcess) processed += 1;
  }
  return processed;
}

export async function processDueInterestAccruals(database, nowDate) {
  const snapshot = await database
    .collection("families")
    .where("interestEnabled", "==", true)
    .limit(MAX_INTEREST_FAMILIES_PER_RUN)
    .get();

  let posted = 0;
  for (const familySnapshot of snapshot.docs) {
    posted += await processFamilyInterestAccrual(database, familySnapshot.ref, familySnapshot.data(), nowDate);
  }
  return posted;
}

async function processRule(database, ruleRef, nowDate) {
  return database.runTransaction(async (transaction) => {
    const ruleSnapshot = await transaction.get(ruleRef);
    if (!ruleSnapshot.exists) return false;

    const rule = ruleSnapshot.data();
    if (!rule.active) return false;

    const nextRunAt = asDate(rule.nextRunAt);
    if (!nextRunAt || nextRunAt > nowDate) return false;

    const validated = validateRule(rule);
    if (!validated.ok) {
      logger.warn("Skipping invalid allowance rule", {
        rulePath: ruleRef.path,
        reason: validated.reason,
      });
      return false;
    }

    const familyRef = ruleRef.parent.parent;
    if (!familyRef) {
      logger.warn("Skipping allowance rule outside a family document", { rulePath: ruleRef.path });
      return false;
    }

    const timeZone = rule.timeZone || DEFAULT_TIME_ZONE;
    const runKey = DateTime.fromJSDate(nextRunAt, { zone: timeZone }).toFormat("yyyyLLdd");
    const ledgerRef = familyRef
      .collection("ledgerTransactions")
      .doc(`allowance_${ruleRef.id}_${runKey}`);
    const ledgerSnapshot = await transaction.get(ledgerRef);

    if (!ledgerSnapshot.exists) {
      transaction.create(ledgerRef, {
        familyId: rule.familyId || familyRef.id,
        childProfileId: rule.childProfileId,
        accountType: rule.accountType || "Main",
        type: "Allowance",
        amountCents: Number(rule.amountCents),
        concept: rule.concept,
        createdBy: "System",
        createdAt: Timestamp.fromDate(nextRunAt),
        source: {
          type: "allowanceRule",
          allowanceRuleId: ruleRef.id,
        },
      });
    }

    transaction.update(ruleRef, {
      nextRunAt: Timestamp.fromDate(nextRunDateAfter(rule, nextRunAt)),
      lastRunAt: Timestamp.fromDate(nextRunAt),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return true;
  });
}

async function processTaskAssignment(database, assignmentRef, nowDate) {
  return database.runTransaction(async (transaction) => {
    const assignmentSnapshot = await transaction.get(assignmentRef);
    if (!assignmentSnapshot.exists) return false;

    const assignment = assignmentSnapshot.data();
    if (!assignment.active) return false;

    const dueAt = asDate(assignment.dueAt);
    if (!dueAt || dueAt > nowDate) return false;

    const validated = validateTaskAssignment(assignment);
    if (!validated.ok) {
      logger.warn("Skipping invalid task assignment", {
        assignmentPath: assignmentRef.path,
        reason: validated.reason,
      });
      return false;
    }

    const familyRef = assignmentRef.parent.parent;
    if (!familyRef) {
      logger.warn("Skipping task assignment outside a family document", { assignmentPath: assignmentRef.path });
      return false;
    }

    const familySnapshot = await transaction.get(familyRef);
    const family = familySnapshot.exists ? familySnapshot.data() : {};
    const timeZone = family.timeZone || DEFAULT_TIME_ZONE;
    const periodKey = taskPeriodKey(dueAt, timeZone);
    const instanceDocs = buildTaskInstanceDocuments({
      familyId: assignment.familyId || familyRef.id,
      assignmentId: assignmentRef.id,
      assignment,
      dueAt,
      periodKey,
      nowDate,
    });

    const instanceRefs = instanceDocs.map((instanceDoc) => familyRef.collection("taskInstances").doc(instanceDoc.id));
    const existingSnapshots = [];
    for (const instanceRef of instanceRefs) {
      existingSnapshots.push(await transaction.get(instanceRef));
    }

    let createdAny = false;
    for (const [index, instanceDoc] of instanceDocs.entries()) {
      const existing = existingSnapshots[index];
      if (!existing.exists) {
        transaction.create(instanceRefs[index], instanceDoc.data);
        createdAny = true;
      }
    }

    const nextDueAt = nextTaskDueDateAfter(assignment, dueAt, timeZone);
    const update = {
      lastRunAt: Timestamp.fromDate(dueAt),
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (nextDueAt) {
      update.dueAt = Timestamp.fromDate(nextDueAt);
    } else {
      update.active = false;
    }
    transaction.update(assignmentRef, update);

    return createdAny || instanceDocs.length > 0;
  });
}

async function processFamilyInterestAccrual(database, familyRef, family, nowDate) {
  const validated = validateInterestFamily(family);
  if (!validated.ok) {
    logger.warn("Skipping invalid interest settings", {
      familyPath: familyRef.path,
      reason: validated.reason,
    });
    return 0;
  }

  const timeZone = family.timeZone || DEFAULT_TIME_ZONE;
  const localNow = DateTime.fromJSDate(nowDate, { zone: timeZone }).startOf("day");
  if (localNow.day !== validated.postingDayOfMonth) return 0;

  const periodKey = localNow.toFormat("yyyyLL");
  const childrenSnapshot = await familyRef.collection("childProfiles").get();
  const childProfileIds = childrenSnapshot.docs
    .filter((childSnapshot) => childSnapshot.exists)
    .map((childSnapshot) => childSnapshot.id);
  if (childProfileIds.length === 0) return 0;

  const ledgerSnapshot = await familyRef.collection("ledgerTransactions").get();
  const balancesByChild = mainBalancesByChild(ledgerSnapshot.docs.map((doc) => doc.data()));
  const docs = buildInterestLedgerDocuments({
    familyId: family.familyId || familyRef.id,
    childProfileIds,
    balancesByChild,
    annualRateBasisPoints: validated.annualRateBasisPoints,
    periodKey,
    postedAt: localNow.toJSDate(),
  });
  if (docs.length === 0) return 0;

  return database.runTransaction(async (transaction) => {
    const ledgerRefs = docs.map((doc) => familyRef.collection("ledgerTransactions").doc(doc.id));
    const existingSnapshots = [];
    for (const ledgerRef of ledgerRefs) {
      existingSnapshots.push(await transaction.get(ledgerRef));
    }

    let created = 0;
    for (const [index, doc] of docs.entries()) {
      if (!existingSnapshots[index].exists) {
        transaction.create(ledgerRefs[index], doc.data);
        created += 1;
      }
    }

    if (created > 0 || family.interestLastPostedPeriodKey !== periodKey) {
      transaction.update(familyRef, {
        interestLastPostedPeriodKey: periodKey,
        interestLastPostedAt: Timestamp.fromDate(nowDate),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    return created;
  });
}

function validateRule(rule) {
  if (!rule.childProfileId || typeof rule.childProfileId !== "string") {
    return { ok: false, reason: "missing childProfileId" };
  }
  if (!rule.concept || typeof rule.concept !== "string") {
    return { ok: false, reason: "missing concept" };
  }
  if (!Number.isInteger(Number(rule.amountCents)) || Number(rule.amountCents) <= 0) {
    return { ok: false, reason: "invalid amountCents" };
  }
  const frequency = normalizeFrequency(rule.frequency);
  if (!frequency) return { ok: false, reason: "invalid frequency" };

  const day = Number(rule.day);
  if (!Number.isInteger(day) || day < 1 || day > 31) {
    return { ok: false, reason: "invalid day" };
  }
  if (frequency === "weekly" && day > 7) {
    return { ok: false, reason: "invalid weekly day" };
  }
  return { ok: true };
}

function validateInterestFamily(family) {
  if (family?.interestEnabled !== true) return { ok: false, reason: "interest disabled" };
  const annualRateBasisPoints = Number(family.interestAnnualRateBasisPoints);
  if (
    !Number.isInteger(annualRateBasisPoints) ||
    annualRateBasisPoints <= 0 ||
    annualRateBasisPoints > 5_000
  ) {
    return { ok: false, reason: "invalid interestAnnualRateBasisPoints" };
  }
  const postingDayOfMonth = Number(family.interestPostingDayOfMonth);
  if (!Number.isInteger(postingDayOfMonth) || postingDayOfMonth < 1 || postingDayOfMonth > 28) {
    return { ok: false, reason: "invalid interestPostingDayOfMonth" };
  }
  return { ok: true, annualRateBasisPoints, postingDayOfMonth };
}

function validateTaskAssignment(assignment) {
  if (!assignment.taskTemplateId || typeof assignment.taskTemplateId !== "string") {
    return { ok: false, reason: "missing taskTemplateId" };
  }
  if (!assignment.title || typeof assignment.title !== "string" || !assignment.title.trim()) {
    return { ok: false, reason: "missing title" };
  }
  if (!Number.isInteger(Number(assignment.rewardCents)) || Number(assignment.rewardCents) <= 0) {
    return { ok: false, reason: "invalid rewardCents" };
  }
  if (typeof assignment.requiresPhoto !== "boolean") {
    return { ok: false, reason: "invalid requiresPhoto" };
  }
  if (!Array.isArray(assignment.childProfileIds) || assignment.childProfileIds.length === 0) {
    return { ok: false, reason: "missing childProfileIds" };
  }
  if (!assignment.childProfileIds.every((childId) => typeof childId === "string" && childId.trim())) {
    return { ok: false, reason: "invalid childProfileIds" };
  }
  const recurrence = normalizeTaskRecurrence(assignment.recurrence);
  if (!recurrence) return { ok: false, reason: "invalid recurrence" };
  if (recurrence === "custom") {
    const interval = Number(assignment.customIntervalDays);
    if (!Number.isInteger(interval) || interval < 1) {
      return { ok: false, reason: "invalid customIntervalDays" };
    }
  }
  return { ok: true };
}

export function buildInterestLedgerDocuments({
  familyId,
  childProfileIds,
  balancesByChild,
  annualRateBasisPoints,
  periodKey,
  postedAt,
}) {
  return childProfileIds
    .map((childProfileId) => {
      const balanceCents = Number(balancesByChild.get(childProfileId) ?? 0);
      const amountCents = calculateMonthlyInterestCents(balanceCents, annualRateBasisPoints);
      if (amountCents <= 0) return null;
      return {
        id: createInterestTransactionId(childProfileId, periodKey),
        data: {
          familyId,
          childProfileId,
          accountType: "Main",
          type: "Interest",
          amountCents,
          concept: `Interes ${periodKey}`,
          createdBy: "System",
          createdAt: Timestamp.fromDate(postedAt),
          source: {
            type: "parentInterest",
            periodKey,
          },
        },
      };
    })
    .filter(Boolean);
}

export function calculateMonthlyInterestCents(balanceCents, annualRateBasisPoints) {
  if (balanceCents <= 0) return 0;
  return Math.floor((Number(balanceCents) * Number(annualRateBasisPoints)) / 10_000 / 12);
}

export function createInterestTransactionId(childProfileId, periodKey) {
  return `interest_${safeDocumentIdPart(childProfileId)}_${periodKey}`;
}

export function mainBalancesByChild(transactions) {
  const balances = new Map();
  for (const transaction of transactions) {
    if (transaction?.accountType !== "Main") continue;
    if (!transaction.childProfileId || !Number.isFinite(Number(transaction.amountCents))) continue;
    balances.set(
      transaction.childProfileId,
      (balances.get(transaction.childProfileId) ?? 0) + Number(transaction.amountCents),
    );
  }
  return balances;
}

export function buildTaskInstanceDocuments({ familyId, assignmentId, assignment, dueAt, periodKey, nowDate }) {
  const uniqueChildIds = [...new Set(assignment.childProfileIds)];
  return uniqueChildIds.map((childProfileId) => ({
    id: createTaskInstanceId(assignmentId, childProfileId, periodKey),
    data: {
      familyId,
      taskAssignmentId: assignmentId,
      taskTemplateId: assignment.taskTemplateId,
      childProfileId,
      title: assignment.title.trim(),
      rewardCents: Number(assignment.rewardCents),
      requiresPhoto: assignment.requiresPhoto,
      status: "Pending",
      dueAt: Timestamp.fromDate(dueAt),
      periodKey,
      createdAt: Timestamp.fromDate(nowDate),
      updatedAt: Timestamp.fromDate(nowDate),
      submittedAt: null,
      reviewedAt: null,
      expiredAt: null,
      photoEvidenceUri: null,
      approvedRewardCents: null,
      approvalTransactionId: null,
      rejectionReason: null,
    },
  }));
}

export function createTaskInstanceId(assignmentId, childProfileId, periodKey) {
  return `task_${safeDocumentIdPart(assignmentId)}_${safeDocumentIdPart(childProfileId)}_${periodKey}`;
}

function nextRunDateAfter(rule, previousRunDate) {
  const frequency = normalizeFrequency(rule.frequency);
  const day = Number(rule.day);
  const timeZone = rule.timeZone || DEFAULT_TIME_ZONE;
  const previous = DateTime.fromJSDate(previousRunDate, { zone: timeZone }).startOf("day");

  if (frequency === "weekly") {
    return previous.plus({ days: 7 }).toJSDate();
  }

  const nextMonth = previous.plus({ months: 1 });
  const targetDay = Math.min(day, nextMonth.daysInMonth);
  return nextMonth.set({
    day: targetDay,
    hour: 0,
    minute: 0,
    second: 0,
    millisecond: 0,
  }).toJSDate();
}

export function nextTaskDueDateAfter(assignment, previousDueDate, timeZone = DEFAULT_TIME_ZONE) {
  const recurrence = normalizeTaskRecurrence(assignment.recurrence);
  if (recurrence === "once") return null;

  const previous = DateTime.fromJSDate(previousDueDate, { zone: timeZone });
  if (recurrence === "daily") {
    return previous.plus({ days: 1 }).toJSDate();
  }
  if (recurrence === "weekly") {
    return previous.plus({ days: 7 }).toJSDate();
  }
  if (recurrence === "custom") {
    return previous.plus({ days: Number(assignment.customIntervalDays) }).toJSDate();
  }
  return null;
}

export function taskPeriodKey(dueAt, timeZone = DEFAULT_TIME_ZONE) {
  return DateTime.fromJSDate(dueAt, { zone: timeZone }).toFormat("yyyyLLdd");
}

function normalizeFrequency(value) {
  const normalized = String(value || "").toLowerCase();
  if (normalized === "weekly") return "weekly";
  if (normalized === "monthly") return "monthly";
  return null;
}

function normalizeTaskRecurrence(value) {
  const normalized = String(value || "").toLowerCase();
  if (normalized === "once") return "once";
  if (normalized === "daily") return "daily";
  if (normalized === "weekly") return "weekly";
  if (normalized === "custom") return "custom";
  return null;
}

function safeDocumentIdPart(value) {
  return String(value || "").replace(/[^A-Za-z0-9_-]/g, "_");
}

function asDate(value) {
  if (!value) return null;
  if (typeof value.toDate === "function") return value.toDate();
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
