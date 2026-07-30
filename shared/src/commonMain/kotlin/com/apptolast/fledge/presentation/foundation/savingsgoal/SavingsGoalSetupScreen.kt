package com.apptolast.fledge.presentation.foundation.savingsgoal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.MoneyPotType
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.savings_goal_setup_account_give
import fledge.shared.generated.resources.savings_goal_setup_account_goal
import fledge.shared.generated.resources.savings_goal_setup_account_label
import fledge.shared.generated.resources.savings_goal_setup_add_image_later
import fledge.shared.generated.resources.savings_goal_setup_back
import fledge.shared.generated.resources.savings_goal_setup_child_label
import fledge.shared.generated.resources.savings_goal_setup_error_invalid_target
import fledge.shared.generated.resources.savings_goal_setup_error_missing_child
import fledge.shared.generated.resources.savings_goal_setup_error_missing_family
import fledge.shared.generated.resources.savings_goal_setup_error_missing_title
import fledge.shared.generated.resources.savings_goal_setup_error_missing_visual
import fledge.shared.generated.resources.savings_goal_setup_icon_bike
import fledge.shared.generated.resources.savings_goal_setup_icon_book
import fledge.shared.generated.resources.savings_goal_setup_icon_game
import fledge.shared.generated.resources.savings_goal_setup_icon_label
import fledge.shared.generated.resources.savings_goal_setup_icon_target
import fledge.shared.generated.resources.savings_goal_setup_pot_give
import fledge.shared.generated.resources.savings_goal_setup_pot_label
import fledge.shared.generated.resources.savings_goal_setup_pot_save
import fledge.shared.generated.resources.savings_goal_setup_save
import fledge.shared.generated.resources.savings_goal_setup_subtitle
import fledge.shared.generated.resources.savings_goal_setup_target_label
import fledge.shared.generated.resources.savings_goal_setup_title
import fledge.shared.generated.resources.savings_goal_setup_title_label
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SavingsGoalSetupScreen(
    childProfileId: ChildProfileId,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SavingsGoalSetupViewModel = koinViewModel(),
) {
    LaunchedEffect(childProfileId) {
        viewModel.load(childProfileId)
    }
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    SavingsGoalSetupContent(
        state = state,
        onTitleChanged = viewModel::updateTitle,
        onTargetAmountChanged = viewModel::updateTargetAmount,
        onPotTypeSelected = viewModel::selectPotType,
        onIconSelected = viewModel::selectIcon,
        onImageSelected = {
            viewModel.selectImage("local://savings-goal/${childProfileId.value}")
        },
        onBack = onBack,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun SavingsGoalSetupContent(
    state: SavingsGoalSetupUiState,
    onTitleChanged: (String) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onPotTypeSelected: (MoneyPotType) -> Unit,
    onIconSelected: (String) -> Unit,
    onImageSelected: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(Res.string.savings_goal_setup_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(Res.string.savings_goal_setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.syncNotice?.let { notice ->
                item {
                    SyncNoticeBanner(notice = notice)
                }
            }
            item {
                SavingsGoalFormCard(
                    state = state,
                    onTitleChanged = onTitleChanged,
                    onTargetAmountChanged = onTargetAmountChanged,
                    onPotTypeSelected = onPotTypeSelected,
                    onIconSelected = onIconSelected,
                    onImageSelected = onImageSelected,
                )
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = savingsGoalSetupErrorText(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            state.operationError?.let {
                item {
                    Text(
                        text = stringResource(Res.string.operation_error_sync),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = onSubmit,
                        enabled = state.canSubmit,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(Res.string.savings_goal_setup_save))
                    }
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.savings_goal_setup_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalFormCard(
    state: SavingsGoalSetupUiState,
    onTitleChanged: (String) -> Unit,
    onTargetAmountChanged: (String) -> Unit,
    onPotTypeSelected: (MoneyPotType) -> Unit,
    onIconSelected: (String) -> Unit,
    onImageSelected: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ReadOnlyBlock(
                label = stringResource(Res.string.savings_goal_setup_child_label),
                value = state.child?.displayName ?: stringResource(Res.string.savings_goal_setup_error_missing_child),
            )
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChanged,
                label = { Text(stringResource(Res.string.savings_goal_setup_title_label)) },
                isError = state.error == SavingsGoalSetupError.MissingTitle,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.targetAmountInput,
                onValueChange = onTargetAmountChanged,
                label = { Text(stringResource(Res.string.savings_goal_setup_target_label)) },
                suffix = { Text(state.currencyCode) },
                isError = state.error == SavingsGoalSetupError.InvalidTarget,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            PotSelector(
                selectedPotType = state.selectedPotType,
                onPotTypeSelected = onPotTypeSelected,
            )
            ReadOnlyBlock(
                label = stringResource(Res.string.savings_goal_setup_account_label),
                value = savingsGoalPotAccountLabel(state.selectedPotType),
            )
            IconSelector(
                selectedIconKey = state.selectedIconKey,
                onIconSelected = onIconSelected,
            )
            OutlinedButton(
                onClick = onImageSelected,
                enabled = !state.isSaving,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Text(stringResource(Res.string.savings_goal_setup_add_image_later))
            }
        }
    }
}

@Composable
private fun PotSelector(selectedPotType: MoneyPotType, onPotTypeSelected: (MoneyPotType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.savings_goal_setup_pot_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavingsGoalPotOption(
                potType = MoneyPotType.Save,
                label = Res.string.savings_goal_setup_pot_save,
                selectedPotType = selectedPotType,
                onPotTypeSelected = onPotTypeSelected,
            )
            SavingsGoalPotOption(
                potType = MoneyPotType.Give,
                label = Res.string.savings_goal_setup_pot_give,
                selectedPotType = selectedPotType,
                onPotTypeSelected = onPotTypeSelected,
            )
        }
    }
}

@Composable
private fun RowScope.SavingsGoalPotOption(
    potType: MoneyPotType,
    label: StringResource,
    selectedPotType: MoneyPotType,
    onPotTypeSelected: (MoneyPotType) -> Unit,
) {
    FilterChip(
        selected = selectedPotType == potType,
        onClick = { onPotTypeSelected(potType) },
        label = { Text(stringResource(label)) },
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
    )
}

@Composable
private fun IconSelector(selectedIconKey: String, onIconSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.savings_goal_setup_icon_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavingsGoalIconOption("target", Res.string.savings_goal_setup_icon_target, selectedIconKey, onIconSelected)
            SavingsGoalIconOption("bike", Res.string.savings_goal_setup_icon_bike, selectedIconKey, onIconSelected)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavingsGoalIconOption("game", Res.string.savings_goal_setup_icon_game, selectedIconKey, onIconSelected)
            SavingsGoalIconOption("book", Res.string.savings_goal_setup_icon_book, selectedIconKey, onIconSelected)
        }
    }
}

@Composable
private fun RowScope.SavingsGoalIconOption(
    iconKey: String,
    label: StringResource,
    selectedIconKey: String,
    onIconSelected: (String) -> Unit,
) {
    FilterChip(
        selected = selectedIconKey == iconKey,
        onClick = { onIconSelected(iconKey) },
        label = { Text(stringResource(label)) },
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
    )
}

@Composable
private fun ReadOnlyBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun savingsGoalPotAccountLabel(potType: MoneyPotType): String = when (potType) {
    MoneyPotType.Spend -> stringResource(Res.string.savings_goal_setup_account_goal)
    MoneyPotType.Save -> stringResource(Res.string.savings_goal_setup_account_goal)
    MoneyPotType.Give -> stringResource(Res.string.savings_goal_setup_account_give)
}

@Composable
private fun savingsGoalSetupErrorText(error: SavingsGoalSetupError): String = when (error) {
    SavingsGoalSetupError.MissingFamily -> stringResource(Res.string.savings_goal_setup_error_missing_family)
    SavingsGoalSetupError.MissingChild -> stringResource(Res.string.savings_goal_setup_error_missing_child)
    SavingsGoalSetupError.MissingTitle -> stringResource(Res.string.savings_goal_setup_error_missing_title)
    SavingsGoalSetupError.InvalidTarget -> stringResource(Res.string.savings_goal_setup_error_invalid_target)
    SavingsGoalSetupError.MissingVisual -> stringResource(Res.string.savings_goal_setup_error_missing_visual)
}

@Preview
@Composable
fun PreviewSavingsGoalSetupContent() {
    FledgeTheme {
        SavingsGoalSetupContent(
            state = SavingsGoalSetupUiState(
                child = ChildProfile(
                    id = ChildProfileId("child-1"),
                    displayName = "Lucas",
                    birthYear = 2017,
                    avatarKey = "rocket",
                ),
                title = "Bici nueva",
                targetAmountInput = "40,00",
                selectedIconKey = "bike",
                syncNotice = null,
            ),
            onTitleChanged = {},
            onTargetAmountChanged = {},
            onPotTypeSelected = {},
            onIconSelected = {},
            onImageSelected = {},
            onBack = {},
            onSubmit = {},
        )
    }
}
