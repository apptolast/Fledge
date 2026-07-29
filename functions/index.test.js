import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import {
  buildApprovalQueueReminderMessage,
  buildTaskInstancePushMessage,
  buildTaskInstanceDocuments,
  createTaskInstanceId,
  nextTaskDueDateAfter,
  processApprovalQueueReminders,
  processDueTaskAssignments,
  taskPeriodKey,
} from "./index.js";

const NOW = new Date("2026-07-29T10:00:00.000Z");
const DUE_AT = new Date("2026-07-29T08:00:00.000Z");

describe("FLE-29 task instance scheduler", () => {
  test("builds deterministic pending instance documents", () => {
    const docs = buildTaskInstanceDocuments({
      familyId: "family-1",
      assignmentId: "assignment-1",
      assignment: assignmentData({ childProfileIds: ["child-1", "child-1", "child-2"] }),
      dueAt: DUE_AT,
      periodKey: "20260729",
      nowDate: NOW,
    });

    assert.deepEqual(docs.map((doc) => doc.id), [
      "task_assignment-1_child-1_20260729",
      "task_assignment-1_child-2_20260729",
    ]);
    assert.equal(docs[0].data.status, "Pending");
    assert.equal(docs[0].data.title, "Poner la mesa");
    assert.equal(docs[0].data.rewardCents, 50);
    assert.equal(docs[0].data.requiresPhoto, false);
    assert.equal(docs[0].data.submittedAt, null);
    assert.equal(docs[0].data.reviewedAt, null);
    assert.equal(docs[0].data.approvedRewardCents, null);
    assert.equal(docs[0].data.approvalTransactionId, null);
    assert.equal(docs[0].data.rejectionReason, null);
  });

  test("calculates recurrence dates without losing local wall clock time", () => {
    assert.equal(taskPeriodKey(DUE_AT, "Europe/Madrid"), "20260729");
    assert.equal(
      nextTaskDueDateAfter(assignmentData({ recurrence: "Daily" }), DUE_AT, "Europe/Madrid").toISOString(),
      "2026-07-30T08:00:00.000Z",
    );
    assert.equal(
      nextTaskDueDateAfter(assignmentData({ recurrence: "Weekly" }), DUE_AT, "Europe/Madrid").toISOString(),
      "2026-08-05T08:00:00.000Z",
    );
    assert.equal(
      nextTaskDueDateAfter(
        assignmentData({ recurrence: "Custom", customIntervalDays: 3 }),
        DUE_AT,
        "Europe/Madrid",
      ).toISOString(),
      "2026-08-01T08:00:00.000Z",
    );
    assert.equal(nextTaskDueDateAfter(assignmentData({ recurrence: "Once" }), DUE_AT, "Europe/Madrid"), null);
  });

  test("creates instances per child and advances due assignments", async () => {
    const database = new FakeDatabase({
      "families/family-1": familyData(),
      "families/family-1/taskAssignments/once": assignmentData({ recurrence: "Once" }),
      "families/family-1/taskAssignments/daily": assignmentData({ recurrence: "Daily" }),
      "families/family-1/taskAssignments/weekly": assignmentData({ recurrence: "Weekly" }),
      "families/family-1/taskAssignments/custom": assignmentData({
        recurrence: "Custom",
        customIntervalDays: 3,
      }),
      "families/family-1/taskAssignments/future": assignmentData({
        dueAt: new Date("2026-07-30T08:00:00.000Z"),
      }),
    });

    const processed = await processDueTaskAssignments(database, NOW);

    assert.equal(processed, 4);
    assert.equal(database.docs.get("families/family-1/taskAssignments/once").active, false);
    assert.equal(
      database.docs.get("families/family-1/taskAssignments/daily").dueAt.toDate().toISOString(),
      "2026-07-30T08:00:00.000Z",
    );
    assert.equal(
      database.docs.get("families/family-1/taskAssignments/weekly").dueAt.toDate().toISOString(),
      "2026-08-05T08:00:00.000Z",
    );
    assert.equal(
      database.docs.get("families/family-1/taskAssignments/custom").dueAt.toDate().toISOString(),
      "2026-08-01T08:00:00.000Z",
    );
    assert.equal(taskInstanceCount(database), 8);
    assert.equal(
      database.docs.get("families/family-1/taskInstances/task_daily_child-1_20260729").status,
      "Pending",
    );
  });

  test("does not duplicate deterministic task instances", async () => {
    const existingInstanceId = createTaskInstanceId("daily", "child-1", "20260729");
    const database = new FakeDatabase({
      "families/family-1": familyData(),
      "families/family-1/taskAssignments/daily": assignmentData({
        recurrence: "Daily",
        childProfileIds: ["child-1"],
      }),
      [`families/family-1/taskInstances/${existingInstanceId}`]: {
        ...taskInstanceData({ taskAssignmentId: "daily", childProfileId: "child-1" }),
      },
    });

    const processed = await processDueTaskAssignments(database, NOW);

    assert.equal(processed, 1);
    assert.equal(taskInstanceCount(database), 1);
    assert.equal(
      database.docs.get("families/family-1/taskAssignments/daily").dueAt.toDate().toISOString(),
      "2026-07-30T08:00:00.000Z",
    );
  });
});

describe("FLE-32 task approval push", () => {
  test("builds parent notification when a task is submitted", () => {
    const message = buildTaskInstancePushMessage({
      before: taskInstanceData({ status: "Pending" }),
      after: taskInstanceData({ status: "Submitted", submittedAt: NOW }),
      params: { familyId: "family-1", taskInstanceId: "task-1" },
      appEnv: "debug",
    });

    assert.equal(message.topic, "fledge_debug_family_family-1_parents");
    assert.deepEqual(message.data, {
      type: "task_submitted",
      familyId: "family-1",
      childProfileId: "child-1",
      taskInstanceId: "task-1",
    });
    assert.equal(message.notification.title, "Tarea lista para revisar");
  });

  test("builds child notification when a task is approved", () => {
    const message = buildTaskInstancePushMessage({
      before: taskInstanceData({ status: "Submitted", submittedAt: NOW }),
      after: taskInstanceData({
        status: "Approved",
        submittedAt: NOW,
        reviewedAt: NOW,
        approvedRewardCents: 75,
      }),
      params: { familyId: "family-1", taskInstanceId: "task-1" },
      appEnv: "release",
    });

    assert.equal(message.topic, "fledge_release_family_family-1_child_child-1");
    assert.deepEqual(message.data, {
      type: "task_approved",
      familyId: "family-1",
      childProfileId: "child-1",
      taskInstanceId: "task-1",
      approvedRewardCents: "75",
    });
    assert.equal(message.notification.title, "Tarea aprobada");
  });

  test("does not notify when task status is unchanged", () => {
    const message = buildTaskInstancePushMessage({
      before: taskInstanceData({ status: "Submitted", submittedAt: NOW }),
      after: taskInstanceData({ status: "Submitted", submittedAt: NOW }),
      params: { familyId: "family-1", taskInstanceId: "task-1" },
      appEnv: "debug",
    });

    assert.equal(message, null);
  });
});

describe("FLE-33 approval queue reminders", () => {
  test("sends one parent reminder when more than five tasks are waiting", async () => {
    const database = new FakeDatabase({
      "families/family-1": familyData(),
      ...Object.fromEntries(
        Array.from({ length: 6 }, (_, index) => [
          `families/family-1/taskInstances/task-${index + 1}`,
          taskInstanceData({
            status: "Submitted",
            submittedAt: new Date("2026-07-29T09:00:00.000Z"),
          }),
        ]),
      ),
    });
    const messaging = new FakeMessaging();

    const processed = await processApprovalQueueReminders(database, messaging, "debug", NOW);

    assert.equal(processed, 1);
    assert.equal(messaging.messages.length, 1);
    assert.equal(messaging.messages[0].topic, "fledge_debug_family_family-1_parents");
    assert.deepEqual(messaging.messages[0].data, {
      type: "approval_queue_reminder",
      familyId: "family-1",
      pendingCount: "6",
      oldestSubmittedAt: "2026-07-29T09:00:00.000Z",
    });
    assert.ok(database.docs.get("families/family-1").approvalQueueReminderLastSentAt);
  });

  test("sends parent reminder when a submitted task is older than seventy two hours", async () => {
    const database = new FakeDatabase({
      "families/family-1": familyData(),
      "families/family-1/taskInstances/task-old": taskInstanceData({
        status: "Submitted",
        submittedAt: new Date("2026-07-25T09:00:00.000Z"),
      }),
      "families/family-1/taskInstances/task-recent": taskInstanceData({
        status: "Submitted",
        submittedAt: new Date("2026-07-29T09:00:00.000Z"),
      }),
    });
    const messaging = new FakeMessaging();

    const processed = await processApprovalQueueReminders(database, messaging, "release", NOW);

    assert.equal(processed, 1);
    assert.equal(messaging.messages.length, 1);
    assert.equal(messaging.messages[0].topic, "fledge_release_family_family-1_parents");
    assert.equal(messaging.messages[0].data.pendingCount, "2");
    assert.equal(messaging.messages[0].data.oldestSubmittedAt, "2026-07-25T09:00:00.000Z");
  });

  test("does not send reminder when queue is below thresholds", async () => {
    const database = new FakeDatabase({
      "families/family-1": familyData(),
      "families/family-1/taskInstances/task-1": taskInstanceData({
        status: "Submitted",
        submittedAt: new Date("2026-07-29T09:00:00.000Z"),
      }),
      "families/family-1/taskInstances/task-2": taskInstanceData({
        status: "Approved",
        submittedAt: new Date("2026-07-25T09:00:00.000Z"),
      }),
    });
    const messaging = new FakeMessaging();

    const processed = await processApprovalQueueReminders(database, messaging, "debug", NOW);

    assert.equal(processed, 0);
    assert.equal(messaging.messages.length, 0);
  });

  test("does not repeat a reminder within the cooldown window", async () => {
    const database = new FakeDatabase({
      "families/family-1": {
        ...familyData(),
        approvalQueueReminderLastSentAt: Timestamp.fromDate(new Date("2026-07-29T09:30:00.000Z")),
      },
      ...Object.fromEntries(
        Array.from({ length: 6 }, (_, index) => [
          `families/family-1/taskInstances/task-${index + 1}`,
          taskInstanceData({
            status: "Submitted",
            submittedAt: new Date("2026-07-25T09:00:00.000Z"),
          }),
        ]),
      ),
    });
    const messaging = new FakeMessaging();

    const processed = await processApprovalQueueReminders(database, messaging, "debug", NOW);

    assert.equal(processed, 0);
    assert.equal(messaging.messages.length, 0);
  });

  test("builds an approval queue reminder payload for parent navigation", () => {
    const message = buildApprovalQueueReminderMessage({
      appEnv: "debug",
      familyId: "family-1",
      pendingCount: 6,
      oldestSubmittedAt: new Date("2026-07-25T09:00:00.000Z"),
    });

    assert.equal(message.topic, "fledge_debug_family_family-1_parents");
    assert.equal(message.notification.title, "Tareas pendientes de revisar");
    assert.deepEqual(message.data, {
      type: "approval_queue_reminder",
      familyId: "family-1",
      pendingCount: "6",
      oldestSubmittedAt: "2026-07-25T09:00:00.000Z",
    });
  });
});

function familyData() {
  return {
    familyId: "family-1",
    timeZone: "Europe/Madrid",
  };
}

function assignmentData({
  familyId = "family-1",
  taskTemplateId = "template-1",
  title = "Poner la mesa",
  rewardCents = 50,
  requiresPhoto = false,
  childProfileIds = ["child-1", "child-2"],
  recurrence = "Daily",
  customIntervalDays = null,
  dueAt = DUE_AT,
  active = true,
} = {}) {
  return {
    familyId,
    taskTemplateId,
    title,
    rewardCents,
    requiresPhoto,
    childProfileIds,
    recurrence,
    customIntervalDays,
    dueAt: Timestamp.fromDate(dueAt),
    active,
  };
}

function taskInstanceData({
  familyId = "family-1",
  taskAssignmentId = "assignment-1",
  taskTemplateId = "template-1",
  childProfileId = "child-1",
  periodKey = "20260729",
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
    requiresPhoto: false,
    status,
    dueAt: Timestamp.fromDate(DUE_AT),
    periodKey,
    createdAt: Timestamp.fromDate(NOW),
    updatedAt: Timestamp.fromDate(NOW),
    submittedAt,
    reviewedAt,
    expiredAt,
    photoEvidenceUri,
    approvedRewardCents,
    approvalTransactionId,
    rejectionReason,
  };
}

function taskInstanceCount(database) {
  return [...database.docs.keys()].filter((path) => path.includes("/taskInstances/")).length;
}

class FakeDatabase {
  constructor(seed) {
    this.docs = new Map(Object.entries(seed).map(([path, data]) => [path, { ...data }]));
  }

  collectionGroup(name) {
    return new FakeQuery(this, name);
  }

  doc(path) {
    return new FakeDocumentRef(this, path);
  }

  async runTransaction(callback) {
    return callback(new FakeTransaction(this));
  }
}

class FakeMessaging {
  constructor() {
    this.messages = [];
  }

  async send(message) {
    this.messages.push(message);
    return `fake-message-${this.messages.length}`;
  }
}

class FakeQuery {
  constructor(database, collectionGroupName, filters = [], maxResults = null) {
    this.database = database;
    this.collectionGroupName = collectionGroupName;
    this.filters = filters;
    this.maxResults = maxResults;
  }

  where(field, operator, value) {
    return new FakeQuery(
      this.database,
      this.collectionGroupName,
      [...this.filters, { field, operator, value }],
      this.maxResults,
    );
  }

  limit(maxResults) {
    return new FakeQuery(this.database, this.collectionGroupName, this.filters, maxResults);
  }

  async get() {
    const docs = [...this.database.docs.entries()]
      .filter(([path]) => path.split("/").at(-2) === this.collectionGroupName)
      .filter(([, data]) => this.filters.every((filter) => matchesFilter(data, filter)))
      .slice(0, this.maxResults ?? undefined)
      .map(([path, data]) => new FakeDocumentSnapshot(new FakeDocumentRef(this.database, path), data));
    return { docs };
  }
}

class FakeTransaction {
  constructor(database) {
    this.database = database;
  }

  async get(ref) {
    return new FakeDocumentSnapshot(ref, this.database.docs.get(ref.path));
  }

  create(ref, data) {
    if (this.database.docs.has(ref.path)) {
      throw new Error(`Document already exists: ${ref.path}`);
    }
    this.database.docs.set(ref.path, { ...data });
  }

  update(ref, data) {
    const existing = this.database.docs.get(ref.path);
    if (!existing) {
      throw new Error(`Document does not exist: ${ref.path}`);
    }
    this.database.docs.set(ref.path, { ...existing, ...data });
  }
}

class FakeDocumentSnapshot {
  constructor(ref, data) {
    this.ref = ref;
    this.id = ref.id;
    this.exists = data !== undefined;
    this._data = data;
  }

  data() {
    return this._data;
  }
}

class FakeDocumentRef {
  constructor(database, path) {
    this.database = database;
    this.path = path;
  }

  get id() {
    return this.path.split("/").at(-1);
  }

  get parent() {
    return new FakeCollectionRef(this.database, this.path.split("/").slice(0, -1).join("/"));
  }

  collection(name) {
    return new FakeCollectionRef(this.database, `${this.path}/${name}`);
  }
}

class FakeCollectionRef {
  constructor(database, path) {
    this.database = database;
    this.path = path;
  }

  get parent() {
    const parts = this.path.split("/");
    if (parts.length < 2) return null;
    return new FakeDocumentRef(this.database, parts.slice(0, -1).join("/"));
  }

  doc(id) {
    return new FakeDocumentRef(this.database, `${this.path}/${id}`);
  }
}

function matchesFilter(data, filter) {
  if (filter.operator === "==") {
    return data[filter.field] === filter.value;
  }
  if (filter.operator === "<=") {
    return timestampMillis(data[filter.field]) <= timestampMillis(filter.value);
  }
  throw new Error(`Unsupported fake query operator: ${filter.operator}`);
}

function timestampMillis(value) {
  if (value && typeof value.toDate === "function") {
    return value.toDate().getTime();
  }
  if (value instanceof Date) {
    return value.getTime();
  }
  return Number.NaN;
}
