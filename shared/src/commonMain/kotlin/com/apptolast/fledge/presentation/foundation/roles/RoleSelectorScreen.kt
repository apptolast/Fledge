package com.apptolast.fledge.presentation.foundation.roles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.navigation.FoundationNavigationTarget
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.role_body
import fledge.shared.generated.resources.role_child_card_subtitle
import fledge.shared.generated.resources.role_child_profiles
import fledge.shared.generated.resources.role_no_children
import fledge.shared.generated.resources.role_parent
import fledge.shared.generated.resources.role_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RoleSelectorScreen(
    onNavigate: (FoundationNavigationTarget) -> Unit,
    viewModel: RoleSelectorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.navigationTarget) {
        state.navigationTarget?.let(onNavigate)
    }

    RoleSelectorContent(
        state = state,
        onParentSelected = viewModel::selectParentMode,
        onChildSelected = viewModel::selectChildMode,
    )
}

@Composable
fun RoleSelectorContent(
    state: RoleSelectorUiState,
    onParentSelected: () -> Unit,
    onChildSelected: (ChildProfileId) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.role_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.role_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onParentSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.role_parent))
            }
            Spacer(Modifier.height(24.dp))
            if (state.childProfiles.isEmpty()) {
                Text(
                    text = stringResource(Res.string.role_no_children),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(Res.string.role_child_profiles),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.childProfiles, key = { it.id.value }) { child ->
                        ChildRoleCard(
                            child = child,
                            onClick = { onChildSelected(child.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildRoleCard(child: ChildProfile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = child.avatarKey.take(2).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(text = child.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(Res.string.role_child_card_subtitle, child.birthYear),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewRoleSelectorContent() {
    FledgeTheme {
        RoleSelectorContent(
            state = RoleSelectorUiState(
                childProfiles = listOf(
                    ChildProfile(
                        id = ChildProfileId("child-1"),
                        displayName = "Lucas",
                        birthYear = 2017,
                        avatarKey = "rocket",
                    ),
                    ChildProfile(
                        id = ChildProfileId("child-2"),
                        displayName = "Sofia",
                        birthYear = 2015,
                        avatarKey = "star",
                    ),
                ),
            ),
            onParentSelected = {},
            onChildSelected = {},
        )
    }
}
