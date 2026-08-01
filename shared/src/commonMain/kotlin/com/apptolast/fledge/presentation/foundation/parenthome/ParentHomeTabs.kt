package com.apptolast.fledge.presentation.foundation.parenthome

import com.apptolast.fledge.presentation.foundation.components.FledgeHomeTabItem
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.home_tab_goals
import fledge.shared.generated.resources.home_tab_goals_cd
import fledge.shared.generated.resources.home_tab_home
import fledge.shared.generated.resources.home_tab_home_cd
import fledge.shared.generated.resources.home_tab_settings
import fledge.shared.generated.resources.home_tab_settings_cd
import fledge.shared.generated.resources.home_tab_tasks
import fledge.shared.generated.resources.home_tab_tasks_cd

enum class ParentHomeTab {
    Home,
    Tasks,
    Goals,
    Settings,
}

enum class ParentHomeSection {
    Header,
    Sync,
    FirstRun,
    Summary,
    GoalNotices,
    ChildOverview,
    Settlements,
    CreateTask,
    TaskApprovals,
    GoalOverview,
    GoalSetup,
    SavingsRules,
    ChildManagement,
    FamilySettings,
    Reports,
}

val parentHomeTabs: List<FledgeHomeTabItem<ParentHomeTab>> = listOf(
    FledgeHomeTabItem(
        tab = ParentHomeTab.Home,
        label = Res.string.home_tab_home,
        contentDescription = Res.string.home_tab_home_cd,
        icon = "⌂",
    ),
    FledgeHomeTabItem(
        tab = ParentHomeTab.Tasks,
        label = Res.string.home_tab_tasks,
        contentDescription = Res.string.home_tab_tasks_cd,
        icon = "☑",
    ),
    FledgeHomeTabItem(
        tab = ParentHomeTab.Goals,
        label = Res.string.home_tab_goals,
        contentDescription = Res.string.home_tab_goals_cd,
        icon = "◎",
    ),
    FledgeHomeTabItem(
        tab = ParentHomeTab.Settings,
        label = Res.string.home_tab_settings,
        contentDescription = Res.string.home_tab_settings_cd,
        icon = "⚙",
    ),
)

fun parentHomeSectionsFor(tab: ParentHomeTab): Set<ParentHomeSection> = when (tab) {
    ParentHomeTab.Home -> setOf(
        ParentHomeSection.Header,
        ParentHomeSection.Sync,
        ParentHomeSection.FirstRun,
        ParentHomeSection.Summary,
        ParentHomeSection.GoalNotices,
        ParentHomeSection.ChildOverview,
        ParentHomeSection.Settlements,
    )
    ParentHomeTab.Tasks -> setOf(
        ParentHomeSection.Header,
        ParentHomeSection.Sync,
        ParentHomeSection.FirstRun,
        ParentHomeSection.CreateTask,
        ParentHomeSection.TaskApprovals,
    )
    ParentHomeTab.Goals -> setOf(
        ParentHomeSection.Header,
        ParentHomeSection.Sync,
        ParentHomeSection.FirstRun,
        ParentHomeSection.GoalNotices,
        ParentHomeSection.GoalOverview,
        ParentHomeSection.GoalSetup,
        ParentHomeSection.SavingsRules,
    )
    ParentHomeTab.Settings -> setOf(
        ParentHomeSection.Header,
        ParentHomeSection.Sync,
        ParentHomeSection.FirstRun,
        ParentHomeSection.ChildManagement,
        ParentHomeSection.FamilySettings,
        ParentHomeSection.Reports,
    )
}
