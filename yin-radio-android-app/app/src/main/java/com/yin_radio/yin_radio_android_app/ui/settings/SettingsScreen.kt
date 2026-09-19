package com.yin_radio.yin_radio_android_app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import android.text.format.DateUtils
import com.yin_radio.yin_radio_android_app.data.local.prefs.ThemeMode
import com.yin_radio.yin_radio_android_app.ui.sync.StationSyncUiState
import com.yin_radio.yin_radio_android_app.ui.sync.StationSyncViewModel
import com.yin_radio.yin_radio_android_app.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    syncViewModel: StationSyncViewModel = koinViewModel()
) {
    DisposableEffect(Unit) {
        Log.v("SettingsScreen", "Entered composition")
        onDispose {
            Log.v("SettingsScreen", "Left composition")
            syncViewModel.reset()
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val syncUiState by syncViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val radioBrowserLink = LinkAnnotation.Url(
        url = "https://www.radio-browser.info",
        styles = TextLinkStyles(
            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
        )
    )

    val githubLink = LinkAnnotation.Url(
        url = "https://github.com/callmekelvin",
        styles = TextLinkStyles(
            style = SpanStyle(color = MaterialTheme.colorScheme.primary)
        )
    )

    Column(modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setThemeMode(mode) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = uiState.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                        ThemeMode.SYSTEM -> "Follow System"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Playback",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Allow HTTP streams")
                Text(
                    text = "Enable playback of stations using unencrypted HTTP streams",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.allowHttpStreams,
                onCheckedChange = { viewModel.setAllowHttpStreams(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Data",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        StationSyncSection(
            syncState = syncUiState,
            onStartSync = { syncViewModel.startSync() }
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yin Radio",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = buildAnnotatedString {
                append("Data sourced from ")
                withLink(radioBrowserLink) {
                    append("Radio Browser")
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = buildAnnotatedString {
                append("Created by ")
                withLink(githubLink) {
                    append("callmekelvin")
                }
            },
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StationSyncSection(
    syncState: StationSyncUiState,
    onStartSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (syncState.isSyncing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Syncing stations...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartSync() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Resync Radio Station Manifest")

                    val subtitle = when {
                        syncState.isComplete -> "Up to date"
                        syncState.lastSyncTimestamp != null -> {
                            val relativeTime = DateUtils.getRelativeTimeSpanString(
                                syncState.lastSyncTimestamp,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            )
                            "Last Updated: $relativeTime"
                        }
                        else -> "Download the Latest Radio Stations Manifest"
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (syncState.isComplete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Radio Station Manifest Sync Complete",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (syncState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = syncState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
