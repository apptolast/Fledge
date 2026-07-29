import { initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { setGlobalOptions } from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { DateTime } from "luxon";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const DEFAULT_TIME_ZONE = "Europe/Madrid";
const MAX_RULES_PER_RUN = 100;

export const runAllowanceRules = makeRunAllowanceRules(getFirestore(), "runAllowanceRules");
export const runAllowanceRulesDebug = makeRunAllowanceRules(getFirestore("debug"), "runAllowanceRulesDebug");

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

function normalizeFrequency(value) {
  const normalized = String(value || "").toLowerCase();
  if (normalized === "weekly") return "weekly";
  if (normalized === "monthly") return "monthly";
  return null;
}

function asDate(value) {
  if (!value) return null;
  if (typeof value.toDate === "function") return value.toDate();
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}
