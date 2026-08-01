package com.apptolast.fledge.presentation.foundation.childhome

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

enum class ChildHomeTab {
    Home,
    Tasks,
    Goals,
    Settings,
}

enum class ChildHomeSection {
    Header,
    Sync,
    Body,
    Pots,
    GoalSummary,
    CompletionNotice,
    CashOut,
    Achievements,
    Tasks,
    GoalDetails,
    InterestProjection,
    Settlements,
    Ledger,
    ProtectedActions,
}

val childHomeTabs: List<FledgeHomeTabItem<ChildHomeTab>> = listOf(
    FledgeHomeTabItem(
        tab = ChildHomeTab.Home,
        label = Res.string.home_tab_home,
        contentDescription = Res.string.home_tab_home_cd,
        icon = "⌂",
    ),
    FledgeHomeTabItem(
        tab = ChildHomeTab.Tasks,
        label = Res.string.home_tab_tasks,
        contentDescription = Res.string.home_tab_tasks_cd,
        icon = "☑",
    ),
    FledgeHomeTabItem(
        tab = ChildHomeTab.Goals,
        label = Res.string.home_tab_goals,
        contentDescription = Res.string.home_tab_goals_cd,
        icon = "◎",
    ),
    FledgeHomeTabItem(
        tab = ChildHomeTab.Settings,
        label = Res.string.home_tab_settings,
        contentDescription = Res.string.home_tab_settings_cd,
        icon = "⚙",
    ),
)

fun childHomeSectionsFor(tab: ChildHomeTab): Set<ChildHomeSection> = when (tab) {
    ChildHomeTab.Home -> setOf(
        ChildHomeSection.Header,
        ChildHomeSection.Sync,
        ChildHomeSection.Body,
        ChildHomeSection.Pots,
        ChildHomeSection.GoalSummary,
        ChildHomeSection.CompletionNotice,
        ChildHomeSection.CashOut,
        ChildHomeSection.Achievements,
    )
    ChildHomeTab.Tasks -> setOf(
        ChildHomeSection.Header,
        ChildHomeSection.Sync,
        ChildHomeSection.Tasks,
    )
    ChildHomeTab.Goals -> setOf(
        ChildHomeSection.Header,
        ChildHomeSection.Sync,
        ChildHomeSection.GoalDetails,
        ChildHomeSection.CompletionNotice,
        ChildHomeSection.InterestProjection,
    )
    ChildHomeTab.Settings -> setOf(
        ChildHomeSection.Header,
        ChildHomeSection.Sync,
        ChildHomeSection.Settlements,
        ChildHomeSection.Ledger,
        ChildHomeSection.ProtectedActions,
    )
}
