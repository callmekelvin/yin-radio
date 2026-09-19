package com.yin_radio.yin_radio_android_app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yin_radio.yin_radio_android_app.R

@Composable
fun FilterChips(
    filters: List<ActiveFilter>,
    onRemoveFilter: ((ActiveFilter) -> Unit)?,
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        Log.v("FilterChips", "Entered composition")
        onDispose { Log.v("FilterChips", "Left composition") }
    }

    if (filters.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = true,
                onClick = {
                    if (onRemoveFilter != null) {
                        Log.v("FilterChips", "onRemoveFilter() - ${filter.displayValue}")
                        onRemoveFilter(filter)
                    }
                },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(filter.displayValue)
                        if (onRemoveFilter != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "Remove filter",
                                modifier = Modifier.width(16.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

data class ActiveFilter(
    val type: FilterType,
    val value: String,
    val displayValue: String
)

enum class FilterType {
    COUNTRY, LANGUAGE, TAG
}
