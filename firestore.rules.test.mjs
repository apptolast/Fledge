import { readFile } from "node:fs/promises";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  Timestamp,
  deleteDoc,
  doc,
  getDoc,
  setDoc,
  updateDoc,
} from "firebase/firestore";

const PROJECT_ID = "fledge-rules-test";
const FAMILY_ID = "parent-1";
const OTHER_FAMILY_ID = "parent-2";
const CHILD_ID = "child-1";
const SIBLING_ID = "child-2";
const NOW = Timestamp.fromDate(new Date("2026-07-29T10:00:00.000Z"));
const LATER = Timestamp.fromDate(new Date("2026-07-29T11:00:00.000Z"));

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: await readFile("firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

function dbFor(uid, token = {}) {
  return testEnv.authenticatedContext(uid, token).firestore();
}

function parentDb(uid = FAMILY_ID) {
  return dbFor(uid);
}

function childDb({
  uid = "child-user",
  familyId = FAMILY_ID,
  childProfileId = CHILD_ID,
} = {}) {
  return dbFor(uid, {
    role: "child",
    familyId,
    childProfileId,
  });
}

async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

function familyPath(familyId = FAMILY_ID) {
  return `families/${familyId}`;
}

function childProfilePath(childProfileId = CHILD_ID, familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/childProfiles/${childProfileId}`;
}

function ledgerPath(transactionId = "tx-1", familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/ledgerTransactions/${transactionId}`;
}

function allowanceRulePath(ruleId = "allowance-1", familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/allowanceRules/${ruleId}`;
}

function taskAssignmentPath(assignmentId = "assignment-1", familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/taskAssignments/${assignmentId}`;
}

function taskInstancePath(instanceId = "task-assignment-1-child-1-20260729", familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/taskInstances/${instanceId}`;
}

function settlementPath(settlementId = "settlement-1", familyId = FAMILY_ID) {
  return `${familyPath(familyId)}/settlements/${settlementId}`;
}

function familyData(familyId = FAMILY_ID) {
  return {
    familyId,
    name: "Familia Garcia",
    currency: "EUR",
    timeZone: "Europe/Madrid",
    moneySettingsLocked: true,
  };
}

function childProfileData(childProfileId = CHILD_ID, familyId = FAMILY_ID) {
  return {
    familyId,
    childProfileId,
    displayName: childProfileId === CHILD_ID ? "Mateo" : "Lucia",
    birthYear: 2016,
    avatarKey: "rocket",
    pinHash: null,
  };
}

function ledgerData({
  familyId = FAMILY_ID,
  childProfileId = CHILD_ID,
  amountCents = 500,
  type = "Bonus",
  createdBy = "Parent",
} = {}) {
  return {
    familyId,
    childProfileId,
    accountType: "Main",
    type,
    amountCents,
    concept: "Tarea completada",
    createdBy,
    createdAt: NOW,
    reversesTransactionId: null,
  };
}

function allowanceRuleData({
  familyId = FAMILY_ID,
  childProfileId = CHILD_ID,
} = {}) {
  return {
    familyId,
    childProfileId,
    accountType: "Main",
    frequency: "Weekly",
    day: 1,
    amountCents: 700,
    concept: "Paga semanal",
    timeZone: "Europe/Madrid",
    nextRunAt: NOW,
    active: true,
    createdAt: NOW,
    updatedAt: NOW,
  };
}

function taskAssignmentData({
  familyId = FAMILY_ID,
  childProfileIds = [CHILD_ID, SIBLING_ID],
  recurrence = "Daily",
  customIntervalDays = null,
} = {}) {
  return {
    familyId,
    taskTemplateId: "template-1",
    title: "Poner la mesa",
    rewardCents: 50,
    requiresPhoto: false,
    childProfileIds,
    recurrence,
    dueAt: LATER,
    customIntervalDays,
    active: true,
    createdAt: NOW,
    updatedAt: NOW,
  };
}

function taskInstanceData({
  familyId = FAMILY_ID,
  taskAssignmentId = "assignment-1",
  taskTemplateId = "template-1",
  childProfileId = CHILD_ID,
  requiresPhoto = false,
  status = "Pending",
  submittedAt = null,
  reviewedAt = null,
  expiredAt = null,
  photoEvidenceUri = null,
  approvedRewardCents = null,
  approvalTransactionId = null,
  rejectionReason = null,
} = {}) {
  return {
    familyId,
    taskAssignmentId,
    taskTemplateId,
    childProfileId,
    title: "Poner la mesa",
    rewardCents: 50,
    requiresPhoto,
    status,
    dueAt: LATER,
    periodKey: "20260729",
    createdAt: NOW,
    updatedAt: NOW,
    submittedAt,
    reviewedAt,
    expiredAt,
    photoEvidenceUri,
    approvedRewardCents,
    approvalTransactionId,
    rejectionReason,
  };
}

function settlementData({
  familyId = FAMILY_ID,
  childProfileId = CHILD_ID,
  amountCents = 500,
  status = "Requested",
  paidByParentAt = null,
  confirmedByChildAt = null,
  settlementTransactionId = null,
} = {}) {
  return {
    familyId,
    childProfileId,
    amountCents,
    concept: "Retirada",
    status,
    requestedAt: NOW,
    paidByParentAt,
    confirmedByChildAt,
    settlementTransactionId,
  };
}

describe("FLE-83 Firestore membership rules", () => {
  test("a parent can manage its family and another user cannot read or write it", async () => {
    const parent = parentDb();
    const otherParent = parentDb(OTHER_FAMILY_ID);

    await assertSucceeds(setDoc(doc(parent, familyPath()), familyData()));
    await assertSucceeds(updateDoc(doc(parent, familyPath()), { name: "Familia Garcia Ruiz" }));

    await assertFails(getDoc(doc(otherParent, familyPath())));
    await assertFails(setDoc(doc(otherParent, familyPath()), familyData()));
    await assertFails(setDoc(doc(otherParent, childProfilePath(SIBLING_ID)), childProfileData(SIBLING_ID)));
  });

  test("a child token can read only its own profile, ledger and allowance", async () => {
    await seed(familyPath(), familyData());
    await seed(childProfilePath(CHILD_ID), childProfileData(CHILD_ID));
    await seed(childProfilePath(SIBLING_ID), childProfileData(SIBLING_ID));
    await seed(ledgerPath("own-ledger"), ledgerData({ childProfileId: CHILD_ID }));
    await seed(ledgerPath("sibling-ledger"), ledgerData({ childProfileId: SIBLING_ID }));
    await seed(allowanceRulePath("own-allowance"), allowanceRuleData({ childProfileId: CHILD_ID }));
    await seed(allowanceRulePath("sibling-allowance"), allowanceRuleData({ childProfileId: SIBLING_ID }));

    const child = childDb();

    await assertSucceeds(getDoc(doc(child, familyPath())));
    await assertSucceeds(getDoc(doc(child, childProfilePath(CHILD_ID))));
    await assertSucceeds(getDoc(doc(child, ledgerPath("own-ledger"))));
    await assertSucceeds(getDoc(doc(child, allowanceRulePath("own-allowance"))));

    await assertFails(getDoc(doc(child, childProfilePath(SIBLING_ID))));
    await assertFails(getDoc(doc(child, ledgerPath("sibling-ledger"))));
    await assertFails(getDoc(doc(child, allowanceRulePath("sibling-allowance"))));
  });

  test("ledger entries are append-only and child tokens cannot write amounts", async () => {
    const parent = parentDb();
    const child = childDb();

    await seed(familyPath(), familyData());
    await assertSucceeds(setDoc(doc(parent, ledgerPath("parent-entry")), ledgerData()));
    await assertFails(updateDoc(doc(parent, ledgerPath("parent-entry")), { amountCents: 10000 }));
    await assertFails(deleteDoc(doc(parent, ledgerPath("parent-entry"))));

    await assertFails(setDoc(doc(child, ledgerPath("child-entry")), ledgerData({ createdBy: "Child" })));
  });

  test("settlement state changes are scoped by role and child ownership", async () => {
    await seed(familyPath(), familyData());
    await seed(settlementPath("pending"), settlementData());
    await seed(
      settlementPath("paid"),
      settlementData({
        status: "PaidByParent",
        paidByParentAt: LATER,
      }),
    );
    await seed(
      settlementPath("paid-tamper"),
      settlementData({
        status: "PaidByParent",
        paidByParentAt: LATER,
      }),
    );

    const parent = parentDb();
    const child = childDb();

    await assertSucceeds(setDoc(doc(child, settlementPath("child-request")), settlementData()));
    await assertFails(setDoc(
      doc(child, settlementPath("sibling-request")),
      settlementData({ childProfileId: SIBLING_ID }),
    ));
    await assertFails(updateDoc(doc(child, settlementPath("pending")), {
      status: "PaidByParent",
      paidByParentAt: LATER,
      confirmedByChildAt: LATER,
      settlementTransactionId: "tx-illegal",
    }));
    await assertSucceeds(updateDoc(doc(parent, settlementPath("pending")), {
      status: "PaidByParent",
      paidByParentAt: LATER,
    }));
    await assertFails(updateDoc(doc(child, settlementPath("paid-tamper")), {
      amountCents: 10000,
      status: "ConfirmedByChild",
      confirmedByChildAt: LATER,
      settlementTransactionId: "tx-tamper",
    }));
    await assertSucceeds(updateDoc(doc(child, settlementPath("paid")), {
      status: "ConfirmedByChild",
      confirmedByChildAt: LATER,
      settlementTransactionId: "tx-settlement",
    }));
  });

  test("only parents can write allowance rules", async () => {
    await seed(familyPath(), familyData());

    const parent = parentDb();
    const child = childDb();

    await assertSucceeds(setDoc(doc(parent, allowanceRulePath()), allowanceRuleData()));
    await assertSucceeds(updateDoc(doc(parent, allowanceRulePath()), {
      amountCents: 800,
      updatedAt: LATER,
    }));
    await assertFails(setDoc(
      doc(child, allowanceRulePath("child-allowance")),
      allowanceRuleData({ childProfileId: CHILD_ID }),
    ));
    await assertFails(updateDoc(doc(child, allowanceRulePath()), {
      amountCents: 10000,
      updatedAt: LATER,
    }));
  });

  test("only parents can write task assignments with an editable snapshot", async () => {
    await seed(familyPath(), familyData());
    await seed(taskAssignmentPath("shared-task"), taskAssignmentData());

    const parent = parentDb();
    const child = childDb();

    await assertSucceeds(getDoc(doc(child, taskAssignmentPath("shared-task"))));
    await assertSucceeds(setDoc(doc(parent, taskAssignmentPath()), taskAssignmentData()));
    await assertSucceeds(setDoc(
      doc(parent, taskAssignmentPath("custom-task")),
      taskAssignmentData({ recurrence: "Custom", customIntervalDays: 3 }),
    ));
    await assertFails(setDoc(
      doc(parent, taskAssignmentPath("blank-title")),
      { ...taskAssignmentData(), title: " " },
    ));
    await assertFails(setDoc(
      doc(parent, taskAssignmentPath("invalid-reward")),
      { ...taskAssignmentData(), rewardCents: 0 },
    ));
    await assertFails(setDoc(
      doc(parent, taskAssignmentPath("invalid-custom")),
      taskAssignmentData({ recurrence: "Custom", customIntervalDays: null }),
    ));
    await assertFails(setDoc(
      doc(child, taskAssignmentPath("child-write")),
      taskAssignmentData({ childProfileIds: [CHILD_ID] }),
    ));
  });

  test("task instances are readable and can only be submitted by the owning child flow", async () => {
    await seed(familyPath(), familyData());
    await seed(taskInstancePath(), taskInstanceData());
    await seed(
      taskInstancePath("task-assignment-1-child-2-20260729"),
      taskInstanceData({ childProfileId: SIBLING_ID }),
    );
    await seed(taskInstancePath("parent-submit"), taskInstanceData());
    await seed(taskInstancePath("needs-photo"), taskInstanceData({ requiresPhoto: true }));
    await seed(taskInstancePath("needs-photo-valid"), taskInstanceData({ requiresPhoto: true }));
    await seed(taskInstancePath("tamper"), taskInstanceData());
    await seed(taskInstancePath("direct-approval"), taskInstanceData());

    const parent = parentDb();
    const child = childDb();
    const sibling = childDb({ childProfileId: SIBLING_ID });

    await assertSucceeds(getDoc(doc(parent, taskInstancePath())));
    await assertSucceeds(getDoc(doc(child, taskInstancePath())));
    await assertFails(getDoc(doc(sibling, taskInstancePath())));
    await assertSucceeds(getDoc(doc(sibling, taskInstancePath("task-assignment-1-child-2-20260729"))));

    await assertFails(setDoc(doc(parent, taskInstancePath("parent-write")), taskInstanceData()));
    await assertFails(setDoc(doc(child, taskInstancePath("child-write")), taskInstanceData()));

    await assertSucceeds(updateDoc(doc(child, taskInstancePath()), {
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
    }));
    await assertSucceeds(updateDoc(doc(parent, taskInstancePath("parent-submit")), {
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(child, taskInstancePath("needs-photo")), {
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
    }));
    await assertSucceeds(updateDoc(doc(child, taskInstancePath("needs-photo-valid")), {
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
      photoEvidenceUri: "local://task-photo-1",
    }));
    await assertFails(updateDoc(doc(child, taskInstancePath("tamper")), {
      rewardCents: 10000,
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(child, taskInstancePath("direct-approval")), {
      status: "Approved",
      submittedAt: LATER,
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(sibling, taskInstancePath("parent-submit")), {
      status: "Submitted",
      submittedAt: LATER,
      updatedAt: LATER,
    }));
  });

  test("submitted task instances can only be approved or rejected by the parent", async () => {
    await seed(familyPath(), familyData());
    await seed(taskInstancePath("approve"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));
    await seed(taskInstancePath("reject"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));
    await seed(taskInstancePath("child-approval"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));
    await seed(taskInstancePath("missing-transaction"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));
    await seed(taskInstancePath("blank-reason"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));
    await seed(taskInstancePath("tamper-approval"), taskInstanceData({ status: "Submitted", submittedAt: NOW }));

    const parent = parentDb();
    const child = childDb();

    await assertSucceeds(updateDoc(doc(parent, taskInstancePath("approve")), {
      status: "Approved",
      approvedRewardCents: 75,
      approvalTransactionId: "tx-task-1",
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertSucceeds(updateDoc(doc(parent, taskInstancePath("reject")), {
      status: "Rejected",
      rejectionReason: "Falta recoger los vasos.",
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(child, taskInstancePath("child-approval")), {
      status: "Approved",
      approvedRewardCents: 50,
      approvalTransactionId: "tx-task-child",
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(parent, taskInstancePath("missing-transaction")), {
      status: "Approved",
      approvedRewardCents: 50,
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(parent, taskInstancePath("blank-reason")), {
      status: "Rejected",
      rejectionReason: " ",
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
    await assertFails(updateDoc(doc(parent, taskInstancePath("tamper-approval")), {
      rewardCents: 10000,
      status: "Approved",
      approvedRewardCents: 75,
      approvalTransactionId: "tx-task-tamper",
      reviewedAt: LATER,
      updatedAt: LATER,
    }));
  });
});
