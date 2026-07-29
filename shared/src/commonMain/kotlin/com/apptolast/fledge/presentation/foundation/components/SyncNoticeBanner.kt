package com.apptolast.fledge.presentation.foundation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apptolast.fledge.presentation.foundation.FoundationSyncNotice
import com.apptolast.fledge.presentation.theme.FledgeTheme
import fledge.shared.generated.resources.Res
import fledge.shared.generated.resources.sync_notice_cached_body
import fledge.shared.generated.resources.sync_notice_cached_title
import fledge.shared.generated.resources.sync_notice_error_body
import fledge.shared.generated.resources.sync_notice_error_title
import fledge.shared.generated.resources.sync_notice_loading_body
import fledge.shared.generated.resources.sync_notice_loading_title
import fledge.shared.generated.resources.sync_notice_syncing_body
import fledge.shared.generated.resources.sync_notice_syncing_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SyncNoticeBanner(notice: FoundationSyncNotice, modifier: Modifier = Modifier) {
    val containerColor = when (notice) {
        FoundationSyncNotice.Error -> MaterialTheme.colorScheme.errorContainer
        FoundationSyncNotice.CachedData,
        FoundationSyncNotice.Loading,
        FoundationSyncNotice.Syncing,
        -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when (notice) {
        FoundationSyncNotice.Error -> MaterialTheme.colorScheme.onErrorContainer
        FoundationSyncNotice.CachedData,
        FoundationSyncNotice.Loading,
        FoundationSyncNotice.Syncing,
        -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = syncNoticeTitle(notice),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = syncNoticeBody(notice),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (notice == FoundationSyncNotice.Loading || notice == FoundationSyncNotice.Syncing) {
                LinearProgressIndicator(
                    color = contentColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun syncNoticeTitle(notice: FoundationSyncNotice): String = when (notice) {
    FoundationSyncNotice.Loading -> stringResource(Res.string.sync_notice_loading_title)
    FoundationSyncNotice.CachedData -> stringResource(Res.string.sync_notice_cached_title)
    FoundationSyncNotice.Syncing -> stringResource(Res.string.sync_notice_syncing_title)
    FoundationSyncNotice.Error -> stringResource(Res.string.sync_notice_error_title)
}

@Composable
private fun syncNoticeBody(notice: FoundationSyncNotice): String = when (notice) {
    FoundationSyncNotice.Loading -> stringResource(Res.string.sync_notice_loading_body)
    FoundationSyncNotice.CachedData -> stringResource(Res.string.sync_notice_cached_body)
    FoundationSyncNotice.Syncing -> stringResource(Res.string.sync_notice_syncing_body)
    FoundationSyncNotice.Error -> stringResource(Res.string.sync_notice_error_body)
}

@Preview
@Composable
fun PreviewSyncNoticeBanner() {
    FledgeTheme {
        SyncNoticeBanner(notice = FoundationSyncNotice.CachedData)
    }
}
