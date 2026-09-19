package com.yin_radio.yin_radio_android_app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yin_radio.yin_radio_android_app.domain.model.Station
import com.yin_radio.yin_radio_android_app.ui.components.ExpandableSearchPanel
import com.yin_radio.yin_radio_android_app.ui.components.StationCard
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onStationClick: (Station, List<Station>) -> Unit,
    onFavoriteClick: (Station) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    DisposableEffect(Unit) {
        Log.v("HomeScreen", "Entered composition")
        onDispose { Log.v("HomeScreen", "Left composition") }
    }

    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                val totalItems = info.totalItemsCount
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                Pair(lastVisible, totalItems)
            }
            .distinctUntilChanged()
            .filter { (lastVisible, total) -> total > 0 && lastVisible >= total - 5 }
            .collect { viewModel.loadMore() }
    }

    LaunchedEffect(uiState.searchQuery, uiState.selectedCountry, uiState.selectedLanguage, uiState.selectedTags) {
        if (uiState.stations.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        ExpandableSearchPanel(
            searchQuery = uiState.searchQuery,
            selectedCountry = uiState.selectedCountry,
            selectedLanguage = uiState.selectedLanguage,
            selectedTags = uiState.selectedTags,
            availableCountries = uiState.availableCountries,
            availableLanguages = uiState.availableLanguages,
            availableTags = uiState.availableTags,
            onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
            onCountrySelected = { viewModel.onCountrySelected(it) },
            onLanguageSelected = { viewModel.onLanguageSelected(it) },
            onTagsSelected = { viewModel.onTagsSelected(it) },
            onSubmitSearch = { viewModel.submitSearch() },
            onClearFilters = { viewModel.clearFilters() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.isLoading && uiState.stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null && uiState.stations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage ?: "Error loading stations")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = uiState.stations,
                    key = { station -> station.stationuuid }
                ) { station ->
                    StationCard(
                        station = station,
                        onClick = { onStationClick(station, uiState.stations) },
                        onFavoriteClick = { onFavoriteClick(station) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
