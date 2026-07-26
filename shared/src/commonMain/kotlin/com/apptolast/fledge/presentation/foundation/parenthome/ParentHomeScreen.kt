package com.apptolast.fledge.presentation.foundation.parenthome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.domain.model.SetupAction
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.empty_children
import fledge.shared.generated.resources.parent_home_balance
import fledge.shared.generated.resources.parent_home_balance_zero
import fledge.shared.generated.resources.parent_home_children
import fledge.shared.generated.resources.parent_home_add_child
import fledge.shared.generated.resources.parent_home_gate
import fledge.shared.generated.resources.parent_home_gate_setup
import fledge.shared.generated.resources.parent_home_pairing
import fledge.shared.generated.resources.parent_home_setup
import fledge.shared.generated.resources.parent_home_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentHomeScreen(
    onPairChild: (ChildProfileId) -> Unit,
    onRequireParentalGate: () -> Unit,
    viewModel: ParentHomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentHomeContent(
        state = state,
        onPairChild = onPairChild,
        onRequireParentalGate = { action ->
            scope.launch {
                viewModel.requestProtectedAction(action)
                onRequireParentalGate()
            }
        },
    )
}

@Composable
fun ParentHomeContent(
    state: ParentHomeUiState,
    onPairChild: (ChildProfileId) -> Unit,
    onRequireParentalGate: (FoundationAction) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.parent_home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            item {
                SummaryCard()
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_home_children),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (state.children.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.empty_children),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.children, key = { it.id.value }) { child ->
                    ChildProfileRow(child = child, onPairChild = onPairChild)
                }
            }
            item {
                Text(
                    text = stringResource(Res.string.parent_home_setup),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(state.setupActions, key = { it.id }) { action ->
                SetupActionRow(action = action, onRequireParentalGate = onRequireParentalGate)
            }
        }
    }
}

@Composable
private fun SummaryCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.parent_home_balance),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.parent_home_balance_zero),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Composable
private fun ChildProfileRow(
    child: ChildProfile,
    onPairChild: (ChildProfileId) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(text = child.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${child.avatarKey} - ${child.birthYear}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(onClick = { onPairChild(child.id) }) {
                Text(stringResource(Res.string.parent_home_pairing))
            }
        }
    }
}

@Composable
private fun SetupActionRow(
    action: SetupAction,
    onRequireParentalGate: (FoundationAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = setupActionLabel(action), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { onRequireParentalGate(FoundationAction.ManageSettings) }) {
                Text(stringResource(Res.string.parent_home_gate))
            }
        }
    }
}

@Composable
private fun setupActionLabel(action: SetupAction): String = when (action.id) {
    "add-child" -> stringResource(Res.string.parent_home_add_child)
    "pair-device" -> stringResource(Res.string.parent_home_pairing)
    else -> stringResource(Res.string.parent_home_gate_setup)
}

@Preview
@Composable
fun PreviewParentHomeContent() {
    FledgeTheme {
        ParentHomeContent(
            state = ParentHomeUiState(
                children = listOf(
                    ChildProfile(
                        id = ChildProfileId("child-1"),
                        displayName = "Lucas",
                        birthYear = 2017,
                        avatarKey = "rocket",
                    )
                )
            ),
            onPairChild = {},
            onRequireParentalGate = {},
        )
    }
}
