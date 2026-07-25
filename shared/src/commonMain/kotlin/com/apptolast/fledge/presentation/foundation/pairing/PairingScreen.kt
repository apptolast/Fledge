package com.apptolast.fledge.presentation.foundation.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.domain.model.ChildProfileId
import com.apptolast.fledge.domain.model.PairingCode
import com.apptolast.fledge.domain.model.PairingSession
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.pairing_body
import fledge.shared.generated.resources.pairing_start
import fledge.shared.generated.resources.pairing_title
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PairingScreen(
    childProfileId: ChildProfileId,
    viewModel: PairingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    PairingContent(
        state = state,
        onStartPairing = {
            scope.launch { viewModel.startPairing(childProfileId) }
        },
    )
}

@Composable
fun PairingContent(
    state: PairingUiState,
    onStartPairing: () -> Unit,
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
                text = stringResource(Res.string.pairing_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.pairing_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = state.pairingSession?.code?.value.orEmpty(),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStartPairing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(stringResource(Res.string.pairing_start))
            }
        }
    }
}

@Preview
@Composable
fun PreviewPairingContent() {
    FledgeTheme {
        PairingContent(
            state = PairingUiState(
                pairingSession = PairingSession(
                    childProfileId = ChildProfileId("child-1"),
                    code = PairingCode("123456"),
                    expiresAt = Clock.System.now().plus(10.minutes),
                )
            ),
            onStartPairing = {},
        )
    }
}
