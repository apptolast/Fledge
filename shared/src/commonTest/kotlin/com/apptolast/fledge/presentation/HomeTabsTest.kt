package com.apptolast.fledge.presentation

import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeSection
import com.apptolast.fledge.presentation.foundation.childhome.ChildHomeTab
import com.apptolast.fledge.presentation.foundation.childhome.childHomeSectionsFor
import com.apptolast.fledge.presentation.foundation.childhome.childHomeTabs
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeSection
import com.apptolast.fledge.presentation.foundation.parenthome.ParentHomeTab
import com.apptolast.fledge.presentation.foundation.parenthome.parentHomeSectionsFor
import com.apptolast.fledge.presentation.foundation.parenthome.parentHomeTabs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeTabsTest {
    @Test
    fun parentHomeTabsMatchPencilOrder() {
        assertEquals(
            listOf(
                ParentHomeTab.Home,
                ParentHomeTab.Tasks,
                ParentHomeTab.Goals,
                ParentHomeTab.Settings,
            ),
            parentHomeTabs.map { it.tab },
        )
    }

    @Test
    fun parentHomeKeepsApprovalAndSettingsOutOfStartTab() {
        val homeSections = parentHomeSectionsFor(ParentHomeTab.Home)

        assertTrue(ParentHomeSection.Summary in homeSections)
        assertTrue(ParentHomeSection.ChildOverview in homeSections)
        assertFalse(ParentHomeSection.TaskApprovals in homeSections)
        assertFalse(ParentHomeSection.ChildManagement in homeSections)
        assertFalse(ParentHomeSection.Reports in homeSections)
    }

    @Test
    fun parentTaskGoalAndSettingsTabsOwnTheirMainWorkflows() {
        assertTrue(ParentHomeSection.TaskApprovals in parentHomeSectionsFor(ParentHomeTab.Tasks))
        assertTrue(ParentHomeSection.CreateTask in parentHomeSectionsFor(ParentHomeTab.Tasks))
        assertTrue(ParentHomeSection.GoalOverview in parentHomeSectionsFor(ParentHomeTab.Goals))
        assertTrue(ParentHomeSection.SavingsRules in parentHomeSectionsFor(ParentHomeTab.Goals))
        assertTrue(ParentHomeSection.ChildManagement in parentHomeSectionsFor(ParentHomeTab.Settings))
        assertTrue(ParentHomeSection.Reports in parentHomeSectionsFor(ParentHomeTab.Settings))
    }

    @Test
    fun childHomeTabsMatchPencilOrder() {
        assertEquals(
            listOf(
                ChildHomeTab.Home,
                ChildHomeTab.Tasks,
                ChildHomeTab.Goals,
                ChildHomeTab.Settings,
            ),
            childHomeTabs.map { it.tab },
        )
    }

    @Test
    fun childHomeKeepsTasksAndProtectedActionsOutOfStartTab() {
        val homeSections = childHomeSectionsFor(ChildHomeTab.Home)

        assertTrue(ChildHomeSection.Pots in homeSections)
        assertTrue(ChildHomeSection.GoalSummary in homeSections)
        assertTrue(ChildHomeSection.CashOut in homeSections)
        assertFalse(ChildHomeSection.Tasks in homeSections)
        assertFalse(ChildHomeSection.ProtectedActions in homeSections)
    }

    @Test
    fun childTaskGoalAndSettingsTabsOwnTheirMainWorkflows() {
        assertTrue(ChildHomeSection.Tasks in childHomeSectionsFor(ChildHomeTab.Tasks))
        assertTrue(ChildHomeSection.GoalDetails in childHomeSectionsFor(ChildHomeTab.Goals))
        assertTrue(ChildHomeSection.InterestProjection in childHomeSectionsFor(ChildHomeTab.Goals))
        assertTrue(ChildHomeSection.Settlements in childHomeSectionsFor(ChildHomeTab.Settings))
        assertTrue(ChildHomeSection.Ledger in childHomeSectionsFor(ChildHomeTab.Settings))
        assertTrue(ChildHomeSection.ProtectedActions in childHomeSectionsFor(ChildHomeTab.Settings))
    }
}
