package com.apptolast.fledge.presentation.foundation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.home_tab_goals
import fledge.shared.generated.resources.home_tab_goals_cd
import fledge.shared.generated.resources.home_tab_home
import fledge.shared.generated.resources.home_tab_home_cd
import fledge.shared.generated.resources.home_tab_settings
import fledge.shared.generated.resources.home_tab_settings_cd
import fledge.shared.generated.resources.home_tab_tasks
import fledge.shared.generated.resources.home_tab_tasks_cd
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

data class FledgeHomeTabItem<T>(
    val tab: T,
    val label: StringResource,
    val contentDescription: StringResource,
    val icon: String,
)

@Composable
fun <T> FledgeHomeTabBar(
    tabs: List<FledgeHomeTabItem<T>>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { item ->
                val selected = item.tab == selectedTab
                val label = stringResource(item.label)
                val tabContentDescription = stringResource(item.contentDescription)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                        .widthIn(min = 64.dp)
                        .selectable(
                            selected = selected,
                            onClick = { onTabSelected(item.tab) },
                            role = Role.Button,
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = tabContentDescription
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = item.icon,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private enum class PreviewTab {
    Home,
    Tasks,
    Goals,
    Settings,
}

@Preview
@Composable
fun PreviewFledgeHomeTabBar() {
    FledgeTheme {
        FledgeHomeTabBar(
            tabs = listOf(
                FledgeHomeTabItem(
                    tab = PreviewTab.Home,
                    label = Res.string.home_tab_home,
                    contentDescription = Res.string.home_tab_home_cd,
                    icon = "⌂",
                ),
                FledgeHomeTabItem(
                    tab = PreviewTab.Tasks,
                    label = Res.string.home_tab_tasks,
                    contentDescription = Res.string.home_tab_tasks_cd,
                    icon = "☑",
                ),
                FledgeHomeTabItem(
                    tab = PreviewTab.Goals,
                    label = Res.string.home_tab_goals,
                    contentDescription = Res.string.home_tab_goals_cd,
                    icon = "◎",
                ),
                FledgeHomeTabItem(
                    tab = PreviewTab.Settings,
                    label = Res.string.home_tab_settings,
                    contentDescription = Res.string.home_tab_settings_cd,
                    icon = "⚙",
                ),
            ),
            selectedTab = PreviewTab.Home,
            onTabSelected = {},
        )
    }
}
