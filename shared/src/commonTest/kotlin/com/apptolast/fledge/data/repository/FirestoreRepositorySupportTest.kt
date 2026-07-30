package com.apptolast.fledge.data.repository

import com.apptolast.fledge.domain.model.AllowanceDay
import com.apptolast.fledge.domain.model.AllowanceFrequency
import com.apptolast.fledge.domain.model.AllowanceRule
import com.apptolast.fledge.domain.model.AllowanceRuleId
import com.apptolast.fledge.domain.model.ChildPinHash
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.InterestSettings
import com.apptolast.fledge.domain.model.LedgerActor
import com.apptolast.fledge.domain.model.LedgerConcept
import com.apptolast.fledge.domain.model.LedgerTransaction
import com.apptolast.fledge.domain.model.LedgerTransactionType
import com.apptolast.fledge.domain.model.LedgerTransferGroupId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.domain.model.SavingsGoal
import com.apptolast.fledge.domain.model.SavingsGoalId
import com.apptolast.fledge.domain.model.SavingsGoalStatus
import com.apptolast.fledge.domain.model.TaskAssignment
import com.apptolast.fledge.domain.model.TaskAssignmentId
import com.apptolast.fledge.domain.model.TaskInstance
import com.apptolast.fledge.domain.model.TaskInstanceId
import com.apptolast.fledge.domain.model.TaskInstanceStatus
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TaskTemplateSource
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.domain.model.TransactionId
import com.apptolast.fledge.domain.model.VirtualAccountType
import com.apptolast.fledge.domain.repository.RepositorySyncStatus
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class FirestoreRepositorySupportTest {

    @Test
    fun `FLE-48 family document stores parent interest settings for scheduler`() {
        // Given
        val family = Family(
            id = FamilyId("family-1"),
            name = "Familia Garcia",
            currency = CurrencyCode("EUR"),
            timeZone = TimeZoneId("Europe/Madrid"),
            interestSettings = InterestSettings(
                enabled = true,
                annualRateBasisPoints = 250,
                postingDayOfMonth = 5,
                lastPostedPeriodKey = "202607",
            ),
        )

        // When
        val data = family.toFirestoreMap()

        // Then
        assertEquals(true, data["interestEnabled"])
        assertEquals(250, data["interestAnnualRateBasisPoints"])
        assertEquals(5, data["interestPostingDayOfMonth"])
        assertEquals("202607", data["interestLastPostedPeriodKey"])
    }

    @Test
    fun `FLE-80 child profile document stores family scoped fields`() {
        // Given
        val familyId = FamilyId("family-1")
        val child = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Leo",
            birthYear = 2018,
            avatarKey = "rocket",
            pinHash = ChildPinHash("a".repeat(64)),
        )

        // When
        val data = child.toFirestoreMap(familyId)

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("child-1", data["childProfileId"])
        assertEquals("Leo", data["displayName"])
        assertEquals(2018, data["birthYear"])
        assertEquals("rocket", data["avatarKey"])
        assertEquals("a".repeat(64), data["pinHash"])
    }

    @Test
    fun `FLE-81 ledger transaction document stores append only fields`() {
        // Given
        val transaction = LedgerTransaction(
            id = TransactionId("tx-1"),
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            accountType = VirtualAccountType.Main,
            type = LedgerTransactionType.Bonus,
            amountCents = MoneyCents(250),
            concept = LedgerConcept("Tarea completada"),
            createdBy = LedgerActor.Parent,
            createdAt = Instant.fromEpochSeconds(1_700_000_000, 123_456_789),
            transferGroupId = LedgerTransferGroupId("transfer-1"),
        )

        // When
        val data = transaction.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("child-1", data["childProfileId"])
        assertEquals("Main", data["accountType"])
        assertEquals("Bonus", data["type"])
        assertEquals(250L, data["amountCents"])
        assertEquals("Tarea completada", data["concept"])
        assertEquals("Parent", data["createdBy"])
        assertIs<Timestamp>(data["createdAt"])
        assertEquals(null, data["reversesTransactionId"])
        assertEquals("transfer-1", data["transferGroupId"])
    }

    @Test
    fun `FLE-82 allowance rule document matches scheduler query schema`() {
        // Given
        val rule = AllowanceRule(
            id = AllowanceRuleId("rule-1"),
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            accountType = VirtualAccountType.Main,
            frequency = AllowanceFrequency.Weekly,
            day = AllowanceDay(5),
            amountCents = MoneyCents(500),
            concept = LedgerConcept("Paga semanal"),
            timeZone = TimeZoneId("Europe/Madrid"),
            nextRunAt = Instant.fromEpochSeconds(1_700_100_000),
            active = true,
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
            updatedAt = Instant.fromEpochSeconds(1_700_000_000),
        )

        // When
        val data = rule.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("child-1", data["childProfileId"])
        assertEquals(true, data["active"])
        assertEquals("Weekly", data["frequency"])
        assertEquals(5, data["day"])
        assertEquals(500L, data["amountCents"])
        assertEquals("Europe/Madrid", data["timeZone"])
        assertIs<Timestamp>(data["nextRunAt"])
    }

    @Test
    fun `FLE-36 savings goal document stores child target and goal account`() {
        // Given
        val goal = SavingsGoal(
            id = SavingsGoalId("goal-1"),
            familyId = FamilyId("family-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Bici nueva",
            targetCents = MoneyCents(4_000),
            accountType = VirtualAccountType.Goal,
            potType = MoneyPotType.Save,
            iconKey = "bike",
            imageUri = null,
            status = SavingsGoalStatus.Active,
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
            updatedAt = Instant.fromEpochSeconds(1_700_000_000),
        )

        // When
        val data = goal.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("child-1", data["childProfileId"])
        assertEquals("Bici nueva", data["title"])
        assertEquals(4_000L, data["targetCents"])
        assertEquals("Goal", data["accountType"])
        assertEquals("Save", data["potType"])
        assertEquals("bike", data["iconKey"])
        assertEquals(null, data["imageUri"])
        assertEquals("Active", data["status"])
        assertIs<Timestamp>(data["createdAt"])
        assertIs<Timestamp>(data["updatedAt"])
    }

    @Test
    fun `AC-05 task template document matches catalog schema`() {
        // Given
        val template = TaskTemplate(
            id = TaskTemplateId("template-1"),
            familyId = FamilyId("family-1"),
            title = "Poner la mesa",
            description = "Preparar platos, vasos y cubiertos.",
            iconKey = "utensils",
            defaultValueCents = MoneyCents(50),
            requiresPhoto = false,
            suggestedMinAge = 7,
            suggestedMaxAge = 9,
            source = TaskTemplateSource.InitialSuggestion,
            sourceChildProfileId = ChildProfileId("child-1"),
            sourceKey = "set-table",
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
            updatedAt = Instant.fromEpochSeconds(1_700_000_000),
        )

        // When
        val data = template.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("Poner la mesa", data["title"])
        assertEquals("Preparar platos, vasos y cubiertos.", data["description"])
        assertEquals("utensils", data["iconKey"])
        assertEquals(50L, data["defaultValueCents"])
        assertEquals(false, data["requiresPhoto"])
        assertEquals(7, data["suggestedMinAge"])
        assertEquals(9, data["suggestedMaxAge"])
        assertEquals("InitialSuggestion", data["source"])
        assertEquals("child-1", data["sourceChildProfileId"])
        assertEquals("set-table", data["sourceKey"])
        assertEquals(false, data["archived"])
        assertIs<Timestamp>(data["createdAt"])
        assertIs<Timestamp>(data["updatedAt"])
    }

    @Test
    fun `AC-05 task assignment document matches assignment schema`() {
        // Given
        val assignment = TaskAssignment(
            id = TaskAssignmentId("assignment-1"),
            familyId = FamilyId("family-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = true,
            childProfileIds = listOf(ChildProfileId("child-1"), ChildProfileId("child-2")),
            recurrence = TaskRecurrence.Custom,
            dueAt = Instant.fromEpochSeconds(1_700_200_000),
            customIntervalDays = 3,
            active = true,
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
            updatedAt = Instant.fromEpochSeconds(1_700_100_000),
        )

        // When
        val data = assignment.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("template-1", data["taskTemplateId"])
        assertEquals("Poner la mesa", data["title"])
        assertEquals(50L, data["rewardCents"])
        assertEquals(true, data["requiresPhoto"])
        assertEquals(listOf("child-1", "child-2"), data["childProfileIds"])
        assertEquals("Custom", data["recurrence"])
        assertIs<Timestamp>(data["dueAt"])
        assertEquals(3, data["customIntervalDays"])
        assertEquals(true, data["active"])
        assertIs<Timestamp>(data["createdAt"])
        assertIs<Timestamp>(data["updatedAt"])
    }

    @Test
    fun `FLE-29 task instance document matches scheduler schema`() {
        // Given
        val instance = TaskInstance(
            id = TaskInstanceId("task-assignment-1-child-1-20260729"),
            familyId = FamilyId("family-1"),
            taskAssignmentId = TaskAssignmentId("assignment-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = false,
            status = TaskInstanceStatus.Pending,
            dueAt = Instant.fromEpochSeconds(1_700_200_000),
            periodKey = "20260729",
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
            updatedAt = Instant.fromEpochSeconds(1_700_100_000),
        )

        // When
        val data = instance.toFirestoreMap()

        // Then
        assertEquals("family-1", data["familyId"])
        assertEquals("assignment-1", data["taskAssignmentId"])
        assertEquals("template-1", data["taskTemplateId"])
        assertEquals("child-1", data["childProfileId"])
        assertEquals("Poner la mesa", data["title"])
        assertEquals(50L, data["rewardCents"])
        assertEquals(false, data["requiresPhoto"])
        assertEquals("Pending", data["status"])
        assertEquals("20260729", data["periodKey"])
        assertIs<Timestamp>(data["dueAt"])
        assertIs<Timestamp>(data["createdAt"])
        assertIs<Timestamp>(data["updatedAt"])
        assertEquals(null, data["submittedAt"])
        assertEquals(null, data["reviewedAt"])
        assertEquals(null, data["expiredAt"])
        assertEquals(null, data["photoEvidenceUri"])
        assertEquals(null, data["approvedRewardCents"])
        assertEquals(null, data["approvalTransactionId"])
        assertEquals(null, data["rejectionReason"])
    }

    @Test
    fun `FLE-30 task instance document stores child submission evidence`() {
        // Given
        val submittedAt = Instant.fromEpochSeconds(1_700_300_000)
        val instance = TaskInstance(
            id = TaskInstanceId("task-assignment-1-child-1-20260729"),
            familyId = FamilyId("family-1"),
            taskAssignmentId = TaskAssignmentId("assignment-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = true,
            status = TaskInstanceStatus.Submitted,
            dueAt = Instant.fromEpochSeconds(1_700_200_000),
            periodKey = "20260729",
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
            updatedAt = submittedAt,
            submittedAt = submittedAt,
            photoEvidenceUri = "local://task-photo-1",
        )

        // When
        val data = instance.toFirestoreMap()

        // Then
        assertEquals("Submitted", data["status"])
        assertEquals("local://task-photo-1", data["photoEvidenceUri"])
        assertIs<Timestamp>(data["submittedAt"])
        assertIs<Timestamp>(data["updatedAt"])
    }

    @Test
    fun `FLE-31 task instance document stores parent approval metadata`() {
        // Given
        val submittedAt = Instant.fromEpochSeconds(1_700_300_000)
        val reviewedAt = Instant.fromEpochSeconds(1_700_300_600)
        val instance = TaskInstance(
            id = TaskInstanceId("task-assignment-1-child-1-20260729"),
            familyId = FamilyId("family-1"),
            taskAssignmentId = TaskAssignmentId("assignment-1"),
            taskTemplateId = TaskTemplateId("template-1"),
            childProfileId = ChildProfileId("child-1"),
            title = "Poner la mesa",
            rewardCents = MoneyCents(50),
            requiresPhoto = false,
            status = TaskInstanceStatus.Approved,
            dueAt = Instant.fromEpochSeconds(1_700_200_000),
            periodKey = "20260729",
            createdAt = Instant.fromEpochSeconds(1_700_100_000),
            updatedAt = reviewedAt,
            submittedAt = submittedAt,
            reviewedAt = reviewedAt,
            approvedRewardCents = MoneyCents(75),
            approvalTransactionId = TransactionId("tx-task-1"),
        )

        // When
        val data = instance.toFirestoreMap()

        // Then
        assertEquals("Approved", data["status"])
        assertEquals(75L, data["approvedRewardCents"])
        assertEquals("tx-task-1", data["approvalTransactionId"])
        assertEquals(null, data["rejectionReason"])
        assertIs<Timestamp>(data["reviewedAt"])
    }

    @Test
    fun `FLE-92 aggregate sync status keeps loading before cache when a collection has not emitted`() {
        // Given
        val statuses = listOf(
            RepositorySyncStatus.FromCache,
            RepositorySyncStatus.Loading,
            RepositorySyncStatus.Synced,
        )

        // When
        val aggregate = statuses.aggregateRepositorySyncStatus()

        // Then
        assertEquals(RepositorySyncStatus.Loading, aggregate)
    }

    @Test
    fun `FLE-92 aggregate sync status reports cache after all initial listeners emitted`() {
        // Given
        val statuses = listOf(
            RepositorySyncStatus.FromCache,
            RepositorySyncStatus.Synced,
            RepositorySyncStatus.Synced,
        )

        // When
        val aggregate = statuses.aggregateRepositorySyncStatus()

        // Then
        assertEquals(RepositorySyncStatus.FromCache, aggregate)
    }
}
