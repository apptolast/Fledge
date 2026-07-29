package com.apptolast.fledge.presentation.foundation.taskassignment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.apptolast.fledge.domain.model.CurrencyCode
import com.apptolast.fledge.domain.model.Family
import com.apptolast.fledge.domain.model.FamilyId
import com.apptolast.fledge.domain.model.MoneyCents
import com.apptolast.fledge.domain.model.TaskRecurrence
import com.apptolast.fledge.domain.model.TaskTemplate
import com.apptolast.fledge.domain.model.TaskTemplateId
import com.apptolast.fledge.domain.model.TimeZoneId
import com.apptolast.fledge.presentation.foundation.components.SyncNoticeBanner
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.operation_error_sync
import fledge.shared.generated.resources.task_assignment_assigned_empty
import fledge.shared.generated.resources.task_assignment_assigned_label
import fledge.shared.generated.resources.task_assignment_back
import fledge.shared.generated.resources.task_assignment_custom_days
import fledge.shared.generated.resources.task_assignment_custom_days_label
import fledge.shared.generated.resources.task_assignment_due_label
import fledge.shared.generated.resources.task_assignment_error_custom_interval
import fledge.shared.generated.resources.task_assignment_error_invalid_reward
import fledge.shared.generated.resources.task_assignment_error_missing_children
import fledge.shared.generated.resources.task_assignment_error_missing_family
import fledge.shared.generated.resources.task_assignment_error_missing_template
import fledge.shared.generated.resources.task_assignment_error_missing_title
import fledge.shared.generated.resources.task_assignment_field_photo
import fledge.shared.generated.resources.task_assignment_field_recurrence
import fledge.shared.generated.resources.task_assignment_field_title
import fledge.shared.generated.resources.task_assignment_field_value
import fledge.shared.generated.resources.task_assignment_no_photo
import fledge.shared.generated.resources.task_assignment_photo_required
import fledge.shared.generated.resources.task_assignment_recurrence_custom
import fledge.shared.generated.resources.task_assignment_recurrence_daily
import fledge.shared.generated.resources.task_assignment_recurrence_once
import fledge.shared.generated.resources.task_assignment_recurrence_weekly
import fledge.shared.generated.resources.task_assignment_save
import fledge.shared.generated.resources.task_assignment_subtitle
import fledge.shared.generated.resources.task_assignment_title
import fledge.shared.generated.resources.task_assignment_use_suggestion
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TaskAssignmentScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TaskAssignmentViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    TaskAssignmentContent(
        state = state,
        onBack = onBack,
        onUseSuggestion = viewModel::useNextSuggestedTemplate,
        onTitleChanged = viewModel::updateTitle,
        onRewardChanged = viewModel::updateReward,
        onRequiresPhotoChanged = viewModel::setRequiresPhoto,
        onChildToggled = viewModel::toggleChild,
        onRecurrenceSelected = viewModel::selectRecurrence,
        onCustomIntervalChanged = viewModel::updateCustomIntervalDays,
        onSubmit = {
            scope.launch {
                if (viewModel.submit()) onSaved()
            }
        },
    )
}

@Composable
fun TaskAssignmentContent(
    state: TaskAssignmentUiState,
    onBack: () -> Unit,
    onUseSuggestion: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onRewardChanged: (String) -> Unit,
    onRequiresPhotoChanged: (Boolean) -> Unit,
    onChildToggled: (ChildProfileId) -> Unit,
    onRecurrenceSelected: (TaskRecurrence) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
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
                        text = stringResource(Res.string.task_assignment_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = stringResource(Res.string.task_assignment_subtitle),
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
                TaskFormCard(
                    state = state,
                    onTitleChanged = onTitleChanged,
                    onRewardChanged = onRewardChanged,
                    onRequiresPhotoChanged = onRequiresPhotoChanged,
                    onChildToggled = onChildToggled,
                    onRecurrenceSelected = onRecurrenceSelected,
                    onCustomIntervalChanged = onCustomIntervalChanged,
                )
            }
            state.error?.let { error ->
                item {
                    Text(
                        text = taskAssignmentErrorText(error),
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
                        Text(stringResource(Res.string.task_assignment_save))
                    }
                    OutlinedButton(
                        onClick = onUseSuggestion,
                        enabled = state.templates.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.task_assignment_use_suggestion))
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(Res.string.task_assignment_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskFormCard(
    state: TaskAssignmentUiState,
    onTitleChanged: (String) -> Unit,
    onRewardChanged: (String) -> Unit,
    onRequiresPhotoChanged: (Boolean) -> Unit,
    onChildToggled: (ChildProfileId) -> Unit,
    onRecurrenceSelected: (TaskRecurrence) -> Unit,
    onCustomIntervalChanged: (String) -> Unit,
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
            EditableTaskField(
                label = stringResource(Res.string.task_assignment_field_title),
                value = state.titleInput,
                onValueChange = onTitleChanged,
                isError = state.error == TaskAssignmentError.MissingTitle,
            )
            EditableTaskField(
                label = stringResource(Res.string.task_assignment_field_value),
                value = state.rewardInput,
                onValueChange = onRewardChanged,
                isError = state.error == TaskAssignmentError.InvalidReward,
                suffix = state.family?.currency?.value ?: "EUR",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            TaskFieldBlock(
                label = stringResource(Res.string.task_assignment_field_recurrence),
                value = recurrenceSummary(state.recurrence),
            )
            RecurrenceSelector(
                selected = state.recurrence,
                onRecurrenceSelected = onRecurrenceSelected,
            )
            if (state.recurrence == TaskRecurrence.Custom) {
                OutlinedTextField(
                    value = state.customIntervalDaysInput,
                    onValueChange = onCustomIntervalChanged,
                    label = { Text(stringResource(Res.string.task_assignment_custom_days_label)) },
                    supportingText = { Text(stringResource(Res.string.task_assignment_custom_days)) },
                    isError = state.error == TaskAssignmentError.InvalidCustomInterval,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TaskFieldBlock(
                label = stringResource(Res.string.task_assignment_assigned_label),
                value = assignedChildrenText(state),
            )
            ChildSelector(
                children = state.children,
                selectedChildProfileIds = state.selectedChildProfileIds,
                onChildToggled = onChildToggled,
            )
            PhotoRequirementSelector(
                label = stringResource(Res.string.task_assignment_field_photo),
                requiresPhoto = state.requiresPhoto,
                onRequiresPhotoChanged = onRequiresPhotoChanged,
            )
        }
    }
}

@Composable
private fun EditableTaskField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TaskFieldBlock(label: String, value: String) {
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
            }
        }
    }
}

@Composable
private fun PhotoRequirementSelector(
    label: String,
    requiresPhoto: Boolean,
    onRequiresPhotoChanged: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !requiresPhoto,
                onClick = { onRequiresPhotoChanged(false) },
                label = { Text(stringResource(Res.string.task_assignment_no_photo)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
            FilterChip(
                selected = requiresPhoto,
                onClick = { onRequiresPhotoChanged(true) },
                label = { Text(stringResource(Res.string.task_assignment_photo_required)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
        }
    }
}

@Composable
private fun RecurrenceSelector(selected: TaskRecurrence, onRecurrenceSelected: (TaskRecurrence) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecurrenceChip(
                recurrence = TaskRecurrence.Once,
                selected = selected,
                onRecurrenceSelected = onRecurrenceSelected,
                modifier = Modifier.weight(1f),
            )
            RecurrenceChip(
                recurrence = TaskRecurrence.Daily,
                selected = selected,
                onRecurrenceSelected = onRecurrenceSelected,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecurrenceChip(
                recurrence = TaskRecurrence.Weekly,
                selected = selected,
                onRecurrenceSelected = onRecurrenceSelected,
                modifier = Modifier.weight(1f),
            )
            RecurrenceChip(
                recurrence = TaskRecurrence.Custom,
                selected = selected,
                onRecurrenceSelected = onRecurrenceSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RecurrenceChip(
    recurrence: TaskRecurrence,
    selected: TaskRecurrence,
    onRecurrenceSelected: (TaskRecurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = recurrence == selected,
        onClick = { onRecurrenceSelected(recurrence) },
        label = { Text(recurrenceLabel(recurrence)) },
        modifier = modifier.height(48.dp),
    )
}

@Composable
private fun ChildSelector(
    children: List<ChildProfile>,
    selectedChildProfileIds: List<ChildProfileId>,
    onChildToggled: (ChildProfileId) -> Unit,
) {
    if (children.isEmpty()) {
        Text(
            text = stringResource(Res.string.task_assignment_assigned_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        children.forEach { child ->
            FilterChip(
                selected = child.id in selectedChildProfileIds,
                onClick = { onChildToggled(child.id) },
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            BoxInitial(child.displayName)
                        }
                        Text(child.displayName, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            )
        }
    }
}

@Composable
private fun BoxInitial(name: String) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun assignedChildrenText(state: TaskAssignmentUiState): String {
    val names = state.children
        .filter { it.id in state.selectedChildProfileIds }
        .joinToString { it.displayName }
    return names.ifBlank { stringResource(Res.string.task_assignment_assigned_empty) }
}

@Composable
private fun recurrenceSummary(recurrence: TaskRecurrence): String =
    "${recurrenceLabel(recurrence)} · ${stringResource(Res.string.task_assignment_due_label)}"

@Composable
private fun recurrenceLabel(recurrence: TaskRecurrence): String = when (recurrence) {
    TaskRecurrence.Once -> stringResource(Res.string.task_assignment_recurrence_once)
    TaskRecurrence.Daily -> stringResource(Res.string.task_assignment_recurrence_daily)
    TaskRecurrence.Weekly -> stringResource(Res.string.task_assignment_recurrence_weekly)
    TaskRecurrence.Custom -> stringResource(Res.string.task_assignment_recurrence_custom)
}

@Composable
private fun taskAssignmentErrorText(error: TaskAssignmentError): String = when (error) {
    TaskAssignmentError.MissingFamily -> stringResource(Res.string.task_assignment_error_missing_family)
    TaskAssignmentError.MissingTemplate -> stringResource(Res.string.task_assignment_error_missing_template)
    TaskAssignmentError.MissingTitle -> stringResource(Res.string.task_assignment_error_missing_title)
    TaskAssignmentError.InvalidReward -> stringResource(Res.string.task_assignment_error_invalid_reward)
    TaskAssignmentError.MissingChildren -> stringResource(Res.string.task_assignment_error_missing_children)
    TaskAssignmentError.InvalidCustomInterval -> stringResource(Res.string.task_assignment_error_custom_interval)
}

@Preview
@Composable
fun PreviewTaskAssignmentContent() {
    FledgeTheme {
        TaskAssignmentContent(
            state = TaskAssignmentUiState(
                family = Family(
                    id = FamilyId("family-1"),
                    name = "Familia Garcia",
                    currency = CurrencyCode("EUR"),
                    timeZone = TimeZoneId("Europe/Madrid"),
                ),
                templates = listOf(
                    TaskTemplate(
                        id = TaskTemplateId("template-1"),
                        familyId = FamilyId("family-1"),
                        title = "Poner la mesa",
                        description = "Preparar platos, vasos y cubiertos.",
                        iconKey = "utensils",
                        defaultValueCents = MoneyCents(50),
                        requiresPhoto = false,
                        createdAt = kotlin.time.Clock.System.now(),
                        updatedAt = kotlin.time.Clock.System.now(),
                    ),
                ),
                children = listOf(
                    ChildProfile(
                        id = ChildProfileId("child-1"),
                        displayName = "Lucia",
                        birthYear = 2017,
                        avatarKey = "star",
                    ),
                    ChildProfile(
                        id = ChildProfileId("child-2"),
                        displayName = "Mateo",
                        birthYear = 2019,
                        avatarKey = "rocket",
                    ),
                ),
                selectedTemplateId = TaskTemplateId("template-1"),
                titleInput = "Poner la mesa",
                rewardInput = "0,50",
                requiresPhoto = false,
                selectedChildProfileIds = listOf(ChildProfileId("child-1"), ChildProfileId("child-2")),
                recurrence = TaskRecurrence.Daily,
                syncNotice = null,
            ),
            onBack = {},
            onUseSuggestion = {},
            onTitleChanged = {},
            onRewardChanged = {},
            onRequiresPhotoChanged = {},
            onChildToggled = {},
            onRecurrenceSelected = {},
            onCustomIntervalChanged = {},
            onSubmit = {},
        )
    }
}
