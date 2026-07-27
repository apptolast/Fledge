package com.apptolast.fledge.presentation.foundation.parentalgate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.FoundationAction
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.parental_gate_body
import fledge.shared.generated.resources.parental_gate_challenge
import fledge.shared.generated.resources.parental_gate_confirm
import fledge.shared.generated.resources.parental_gate_error_missing
import fledge.shared.generated.resources.parental_gate_error_wrong
import fledge.shared.generated.resources.parental_gate_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentalGateScreen(onConfirmed: (FoundationAction) -> Unit, viewModel: ParentalGateViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    ParentalGateContent(
        state = state,
        onAnswerChange = viewModel::updateAnswer,
        onConfirm = {
            scope.launch {
                viewModel.confirmGate()?.let(onConfirmed)
            }
        },
    )
}

@Composable
fun ParentalGateContent(state: ParentalGateUiState, onAnswerChange: (String) -> Unit, onConfirm: () -> Unit) {
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
                text = stringResource(Res.string.parental_gate_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(Res.string.parental_gate_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = state.answer,
                onValueChange = onAnswerChange,
                label = {
                    Text(stringResource(Res.string.parental_gate_challenge, state.challenge))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (error) {
                        ParentalGateError.WrongAnswer -> stringResource(Res.string.parental_gate_error_wrong)
                        ParentalGateError.MissingRequest -> stringResource(Res.string.parental_gate_error_missing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onConfirm,
                enabled = state.answer.isNotBlank(),
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.parental_gate_confirm))
            }
        }
    }
}

@Preview
@Composable
fun PreviewParentalGateContent() {
    FledgeTheme {
        ParentalGateContent(
            state = ParentalGateUiState(answer = "12"),
            onAnswerChange = {},
            onConfirm = {},
        )
    }
}
