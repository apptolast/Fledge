import { initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import * as logger from "firebase-functions/logger";
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { DateTime } from "luxon";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const DEFAULT_TIME_ZONE = "Europe/Madrid";
const MAX_RULES_PER_RUN = 100;
const MAX_TASK_ASSIGNMENTS_PER_RUN = 100;
const MAX_TASK_EXPIRATIONS_PER_RUN = 500;
const MAX_APPROVAL_REMINDER_INSTANCES_PER_RUN = 500;
const APPROVAL_QUEUE_COUNT_THRESHOLD = 5;
const APPROVAL_QUEUE_STALE_HOURS = 72;
const APPROVAL_QUEUE_REMINDER_COOLDOWN_HOURS = 24;

export const runAllowanceRules = makeRunAllowanceRules(getFirestore(), "runAllowanceRules");
export const runAllowanceRulesDebug = makeRunAllowanceRules(getFirestore("debug"), "runAllowanceRulesDebug");
export const runTaskAssignments = makeRunTaskAssignments(getFirestore(), "runTaskAssignments");
export const runTaskAssignmentsDebug = makeRunTaskAssignments(getFirestore("debug"), "runTaskAssignmentsDebug");
export const runTaskInstanceExpirations = makeRunTaskInstanceExpirations(
  getFirestore(),
  "runTaskInstanceExpirations",
);
export const runTaskInstanceExpirationsDebug = makeRunTaskInstanceExpirations(
  getFirestore("debug"),
  "runTaskInstanceExpirationsDebug",
);
export const runApprovalQueueReminders = makeRunApprovalQueueReminders(
  getFirestore(),
  getMessaging(),
  "release",
  "runApprovalQueueReminders",
);
export const runApprovalQueueRemindersDebug = makeRunApprovalQueueReminders(
  getFirestore("debug"),
  getMessaging(),
  "debug",
  "runApprovalQueueRemindersDebug",
);
export const notifyTaskInstancePush = makeNotifyTaskInstancePush(
  getMessaging(),
  "release",
  "(default)",
  "notifyTaskInstancePush",
);
export const notifyTaskInstancePushDebug = makeNotifyTaskInstancePush(
  getMessaging(),
  "debug",
  "debug",
  "notifyTaskInstancePushDebug",
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

function makeRunTaskInstanceExpirations(database, functionName) {
  return onSchedule(
    {
      schedule: "every 1 hours",
      timeZone: DEFAULT_TIME_ZONE,
    },
    async () => {
      const processed = await processExpiredTaskInstances(database, new Date());
      logger.info(`${functionName} completed`, { processed });
    },
  );
}

function makeRunApprovalQueueReminders(database, messaging, appEnv, functionName) {
  return onSchedule(
    {
      schedule: "every 6 hours",
      timeZone: DEFAULT_TIME_ZONE,
    },
    async () => {
      const processed = await processApprovalQueueReminders(database, messaging, appEnv, new Date());
      logger.info(`${functionName} completed`, { processed });
    },
  );
}

function makeNotifyTaskInstancePush(messaging, appEnv, databaseId, functionName) {
  return onDocumentWritten(
    {
      document: "families/{familyId}/taskInstances/{taskInstanceId}",
      database: databaseId,
    },
    async (event) => {
      const message = buildTaskInstancePushMessage({
        before: event.data?.before?.data() ?? null,
        after: event.data?.after?.data() ?? null,
        params: event.params,
        appEnv,
      });
      if (!message) return;

      await messaging.send(message);
      logger.info(`${functionName} sent`, {
        type: message.data.type,
        topic: message.topic,
        familyId: message.data.familyId,
        taskInstanceId: message.data.taskInstanceId,
      });
    },
  );
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

export async function processExpiredTaskInstances(database, nowDate) {
  const now = Timestamp.fromDate(nowDate);
  const snapshot = await database
    .collectionGroup("taskInstances")
    .where("status", "in", ["Pending", "Rejected"])
    .where("dueAt", "<=", now)
    .limit(MAX_TASK_EXPIRATIONS_PER_RUN)
    .get();

  let processed = 0;
  for (const instanceSnapshot of snapshot.docs) {
    const didProcess = await processTaskInstanceExpiration(database, instanceSnapshot.ref, nowDate);
    if (didProcess) processed += 1;
  }
  return processed;
}

export async function processApprovalQueueReminders(database, messaging, appEnv, nowDate) {
  const snapshot = await database
    .collectionGroup("taskInstances")
    .where("status", "==", "Submitted")
    .limit(MAX_APPROVAL_REMINDER_INSTANCES_PER_RUN)
    .get();

  const queuesByFamily = submittedTaskQueuesByFamily(snapshot.docs, nowDate);
  let processed = 0;

  for (const queue of queuesByFamily.values()) {
    if (!shouldSendApprovalQueueReminder(queue, nowDate)) continue;

    const familyRef = database.doc(`families/${queue.familyId}`);
    const shouldSend = await database.runTransaction(async (transaction) => {
      const familySnapshot = await transaction.get(familyRef);
      if (!familySnapshot.exists) {
        logger.warn("Skipping approval queue reminder for missing family", { familyId: queue.familyId });
        return false;
      }

      const family = familySnapshot.data();
      const lastSentAt = asDate(family.approvalQueueReminderLastSentAt);
      if (lastSentAt && hoursBetween(lastSentAt, nowDate) < APPROVAL_QUEUE_REMINDER_COOLDOWN_HOURS) {
        return false;
      }

      transaction.update(familyRef, {
        approvalQueueReminderLastSentAt: Timestamp.fromDate(nowDate),
        updatedAt: FieldValue.serverTimestamp(),
      });
      return true;
    });

    if (!shouldSend) continue;

    await messaging.send(
      buildApprovalQueueReminderMessage({
        appEnv,
        familyId: queue.familyId,
        pendingCount: queue.pendingCount,
        oldestSubmittedAt: queue.oldestSubmittedAt,
      }),
    );
    processed += 1;
  }

  return processed;
}

async function processTaskInstanceExpiration(database, instanceRef, nowDate) {
  return database.runTransaction(async (transaction) => {
    const instanceSnapshot = await transaction.get(instanceRef);
    if (!instanceSnapshot.exists) return false;

    const instance = instanceSnapshot.data();
    if (!["Pending", "Rejected"].includes(instance.status)) return false;

    const dueAt = asDate(instance.dueAt);
    if (!dueAt || dueAt > nowDate) return false;

    transaction.update(instanceRef, {
      status: "Expired",
      updatedAt: Timestamp.fromDate(nowDate),
      expiredAt: Timestamp.fromDate(nowDate),
      submittedAt: null,
      reviewedAt: null,
      photoEvidenceUri: null,
      approvedRewardCents: null,
      approvalTransactionId: null,
      rejectionReason: null,
    });
    return true;
  });
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

export function buildTaskInstancePushMessage({ before, after, params, appEnv = "debug" }) {
  if (!after) return null;
  const previousStatus = before?.status ?? null;
  const nextStatus = after.status ?? null;
  if (!nextStatus || previousStatus === nextStatus) return null;

  const familyId = asNonBlankString(after.familyId) || asNonBlankString(params?.familyId);
  const childProfileId = asNonBlankString(after.childProfileId);
  const taskInstanceId = asNonBlankString(params?.taskInstanceId);
  const title = asNonBlankString(after.title) || "la tarea";
  if (!familyId || !childProfileId || !taskInstanceId) return null;

  if (nextStatus === "Submitted") {
    return withDefaultPushOptions({
      topic: taskPushTopic({
        appEnv,
        familyId,
        audience: "parents",
      }),
      notification: {
        title: "Tarea lista para revisar",
        body: `Revisa "${title}" en la cola de aprobacion.`,
      },
      data: {
        type: "task_submitted",
        familyId,
        childProfileId,
        taskInstanceId,
      },
    });
  }

  if (nextStatus === "Approved") {
    const approvedRewardCents = Number(after.approvedRewardCents);
    if (!Number.isInteger(approvedRewardCents) || approvedRewardCents <= 0) return null;
    return withDefaultPushOptions({
      topic: taskPushTopic({
        appEnv,
        familyId,
        childProfileId,
        audience: "child",
      }),
      notification: {
        title: "Tarea aprobada",
        body: `Has ganado saldo por "${title}".`,
      },
      data: {
        type: "task_approved",
        familyId,
        childProfileId,
        taskInstanceId,
        approvedRewardCents: String(approvedRewardCents),
      },
    });
  }

  return null;
}

export function buildApprovalQueueReminderMessage({ appEnv = "debug", familyId, pendingCount, oldestSubmittedAt }) {
  return withDefaultPushOptions({
    topic: taskPushTopic({
      appEnv,
      familyId,
      audience: "parents",
    }),
    notification: {
      title: "Tareas pendientes de revisar",
      body: approvalQueueReminderBody(pendingCount),
    },
    data: {
      type: "approval_queue_reminder",
      familyId,
      pendingCount: String(pendingCount),
      oldestSubmittedAt: oldestSubmittedAt.toISOString(),
    },
  });
}

export function taskPushTopic({ appEnv = "debug", familyId, childProfileId = null, audience }) {
  const base = `fledge_${normalizeAppEnv(appEnv)}_family_${safeTopicPart(familyId)}`;
  if (audience === "parents") return `${base}_parents`;
  if (audience === "child") return `${base}_child_${safeTopicPart(childProfileId)}`;
  throw new Error(`Unsupported task push audience: ${audience}`);
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

function submittedTaskQueuesByFamily(docs, nowDate) {
  const queues = new Map();
  for (const doc of docs) {
    const data = doc.data();
    const submittedAt = asDate(data.submittedAt);
    if (!submittedAt || submittedAt > nowDate) continue;

    const familyId = doc.ref?.parent?.parent?.id || asNonBlankString(data.familyId);
    if (!familyId) continue;

    const queue = queues.get(familyId) ?? {
      familyId,
      pendingCount: 0,
      oldestSubmittedAt: submittedAt,
    };
    queue.pendingCount += 1;
    if (submittedAt < queue.oldestSubmittedAt) {
      queue.oldestSubmittedAt = submittedAt;
    }
    queues.set(familyId, queue);
  }
  return queues;
}

function shouldSendApprovalQueueReminder(queue, nowDate) {
  if (queue.pendingCount > APPROVAL_QUEUE_COUNT_THRESHOLD) return true;
  return hoursBetween(queue.oldestSubmittedAt, nowDate) >= APPROVAL_QUEUE_STALE_HOURS;
}

function approvalQueueReminderBody(pendingCount) {
  if (pendingCount === 1) {
    return "Hay 1 tarea esperando aprobacion.";
  }
  return `Hay ${pendingCount} tareas esperando aprobacion.`;
}

function hoursBetween(from, to) {
  return (to.getTime() - from.getTime()) / (60 * 60 * 1000);
}

function safeDocumentIdPart(value) {
  return String(value || "").replace(/[^A-Za-z0-9_-]/g, "_");
}

function safeTopicPart(value) {
  return String(value || "").replace(/[^A-Za-z0-9_-]/g, "_");
}

function normalizeAppEnv(value) {
  return String(value || "").toLowerCase() === "release" ? "release" : "debug";
}

function withDefaultPushOptions(message) {
  return {
    ...message,
    android: {
      priority: "high",
    },
    apns: {
      payload: {
        aps: {
          sound: "default",
        },
      },
    },
  };
}

function asNonBlankString(value) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function asDate(value) {
  if (!value) return null;
  if (typeof value.toDate === "function") return value.toDate();
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
