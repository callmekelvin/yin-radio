package com.yin_radio.yin_radio_android_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
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
fun StationCard(
    station: Station,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                Log.v("StationCard", "onClick() - ${station.name}")
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station.favicon,
                contentDescription = station.name,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.default_station),
                error = painterResource(R.drawable.default_station),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        if (station.country.isNotBlank()) append(station.country)
                        if (station.country.isNotBlank() && station.tags.isNotEmpty()) append(" · ")
                        if (station.tags.isNotEmpty()) append(station.tags.firstOrNull() ?: "")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = {
                Log.v("StationCard", "onFavoriteClick() - ${station.name}")
                onFavoriteClick()
            }) {
                Icon(
                    painter = if (station.isFavorite) painterResource(R.drawable.ic_favorite) else painterResource(R.drawable.ic_favorite_border),
                    contentDescription = if (station.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (station.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
