package com.yin_radio.yin_radio_android_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yin_radio.yin_radio_android_app.R
import com.yin_radio.yin_radio_android_app.domain.model.Station

@Composable
fun NowPlayingBar(
    station: Station?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        Log.v("NowPlayingBar", "Entered composition")
        onDispose { Log.v("NowPlayingBar", "Left composition") }
    }

    if (station == null) return

    // Render Now Playing as a Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable {
            Log.v("NowPlayingBar", "onClick()")
            onClick()
        },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // Display within the same Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Display Station Icon/ Placeholder Icon
            AsyncImage(
                model = station.favicon,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.default_station),
                error = painterResource(R.drawable.default_station),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Display Station Name Test
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
