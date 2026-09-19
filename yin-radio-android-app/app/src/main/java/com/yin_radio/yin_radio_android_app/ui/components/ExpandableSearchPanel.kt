package com.yin_radio.yin_radio_android_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.yin_radio.yin_radio_android_app.R
import com.yin_radio.yin_radio_android_app.data.local.db.TagEntity

@Composable
fun ExpandableSearchPanel(
    searchQuery: String,
    selectedCountry: String,
    selectedLanguage: String,
    selectedTags: List<String>,
    availableCountries: List<String>,
    availableLanguages: List<String>,
    availableTags: List<TagEntity>,
    onSearchQueryChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onTagsSelected: (List<String>) -> Unit,
    onSubmitSearch: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    var draftQuery by remember { mutableStateOf(searchQuery) }
    var draftCountry by remember { mutableStateOf(selectedCountry) }
    var draftLanguage by remember { mutableStateOf(selectedLanguage) }
    var draftTags by remember { mutableStateOf(selectedTags) }

    var showCountryDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            draftQuery = searchQuery
            draftCountry = selectedCountry
            draftLanguage = selectedLanguage
            draftTags = selectedTags
        }
    }

    fun confirm() {
        onSearchQueryChange(draftQuery)
        onCountrySelected(draftCountry)
        onLanguageSelected(draftLanguage)
        onTagsSelected(draftTags)
        onSubmitSearch()
        isExpanded = false
    }

    fun clear() {
        onClearFilters()
        isExpanded = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search Bar
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = if (isExpanded) draftQuery else searchQuery,
                onValueChange = {
                    if (isExpanded) draftQuery = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search stations...") },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    val currentQuery = if (isExpanded) draftQuery else searchQuery
                    if (currentQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            if (isExpanded) {
                                draftQuery = ""
                            } else {
                                onSearchQueryChange("")
                                onSubmitSearch()
                            }
                        }) {
                            Icon(painterResource(R.drawable.ic_clear), contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                enabled = isExpanded,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { confirm() })
            )
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { isExpanded = true }
                )
            }
        }

        // Active filters shown in both states
        val activeFilters = buildList {
            if (selectedCountry.isNotEmpty()) {
                add(ActiveFilter(FilterType.COUNTRY, selectedCountry, selectedCountry))
            }
            if (selectedLanguage.isNotEmpty()) {
                add(ActiveFilter(FilterType.LANGUAGE, selectedLanguage, selectedLanguage))
            }
            selectedTags.forEach { tag ->
                add(ActiveFilter(FilterType.TAG, tag, tag))
            }
        }

        if (activeFilters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FilterChips(
                filters = activeFilters,
                onRemoveFilter = null,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(12.dp))

            // Country Row
            FilterRow(
                label = "Country",
                value = draftCountry,
                placeholder = "+ Select",
                onClick = { showCountryDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Language Row
            FilterRow(
                label = "Language",
                value = draftLanguage,
                placeholder = "+ Select",
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tags Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tags:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(72.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    draftTags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                draftTags = draftTags - tag
                            },
                            label = { Text(tag) }
                        )
                    }
                    AssistChip(
                        onClick = { showTagDialog = true },
                        label = { Text("+ Add") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = { clear() }) {
                    Text("Clear Filters")
                }
                TextButton(onClick = { confirm() }) {
                    Text("Confirm")
                }
            }
        }
    }

    if (showCountryDialog) {
        FilterSelectionDialog(
            title = "Select Country",
            items = availableCountries,
            selectedItem = draftCountry,
            onItemSelected = {
                draftCountry = it
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    if (showLanguageDialog) {
        FilterSelectionDialog(
            title = "Select Language",
            items = availableLanguages,
            selectedItem = draftLanguage,
            onItemSelected = {
                draftLanguage = it
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showTagDialog) {
        TagSelectionDialog(
            title = "Select Tags",
            items = availableTags.map { it.tagName },
            selectedItems = draftTags,
            onItemToggled = { tag ->
                draftTags = if (draftTags.contains(tag)) {
                    draftTags - tag
                } else {
                    draftTags + tag
                }
            },
            onDismiss = { showTagDialog = false }
        )
    }
}

@Composable
private fun FilterRow(
    label: String,
    value: String,
    placeholder: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp)
        )
        AssistChip(
            onClick = onClick,
            label = { Text(if (value.isNotEmpty()) value else placeholder) }
        )
    }
}

@Composable
private fun FilterSelectionDialog(
    title: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items) { item ->
                    val isSelected = item == selectedItem
                    TextButton(
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TagSelectionDialog(
    title: String,
    items: List<String>,
    selectedItems: List<String>,
    onItemToggled: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemToggled(item) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedItems.contains(item),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(item)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
