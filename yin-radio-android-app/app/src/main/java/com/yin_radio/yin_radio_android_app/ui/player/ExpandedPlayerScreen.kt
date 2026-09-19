package com.yin_radio.yin_radio_android_app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yin_radio.yin_radio_android_app.R
import com.yin_radio.yin_radio_android_app.domain.model.Station

@Composable
fun ExpandedPlayerScreen(
    station: Station?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        Log.v("ExpandedPlayerScreen", "Entered composition")
        onDispose { Log.v("ExpandedPlayerScreen", "Left composition") }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                }
                IconButton(onClick = { /* share */ }) {
                    Icon(painterResource(R.drawable.ic_share), contentDescription = "Share")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AsyncImage(
                model = station?.favicon,
                contentDescription = station?.name,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.default_station),
                error = painterResource(R.drawable.default_station),
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = station?.name ?: "Select a station",
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = station?.let { "${it.country} \u00B7 ${it.tags.firstOrNull() ?: ""}" } ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Previous", modifier = Modifier.size(40.dp))
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        painter = if (isPlaying) painterResource(R.drawable.ic_pause) else painterResource(R.drawable.ic_play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Next", modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    painter = if (station?.isFavorite == true) painterResource(R.drawable.ic_favorite) else painterResource(R.drawable.ic_favorite_border),
                    contentDescription = "Favorite",
                    tint = if (station?.isFavorite == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
