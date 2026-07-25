package com.apptolast.fledge.presentation.foundation.parentalgate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.parental_gate_body
import fledge.shared.generated.resources.parental_gate_confirm
import fledge.shared.generated.resources.parental_gate_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ParentalGateScreen(
    onConfirmed: () -> Unit,
    viewModel: ParentalGateViewModel = koinViewModel(),
) {
    ParentalGateContent(
        onConfirm = {
            viewModel.confirmGate()
            onConfirmed()
        }
    )
}

@Composable
fun ParentalGateContent(onConfirm: () -> Unit) {
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
            Button(
                onClick = onConfirm,
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
        ParentalGateContent(onConfirm = {})
    }
}
