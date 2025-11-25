package com.cs407.brickcollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cs407.brickcollector.R
import com.cs407.brickcollector.models.LegoSet
import com.cs407.brickcollector.api.ApiService

@Composable
fun MySetsScreen(
    onNavigateToSettings: () -> Unit = {}
) {
    // State for the list - fetched from API
    var itemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var showFilterWidget by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<LegoSet?>(null) }

    // Pagination variables
    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }

    // Filter state variables
    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    var dateAcquired by remember { mutableStateOf("") }
    var fillerField1 by remember { mutableStateOf("") }
    var fillerField2 by remember { mutableStateOf("") }
    var starWarsChecked by remember { mutableStateOf(false) }
    var indianaJonesChecked by remember { mutableStateOf(false) }
    var harryPotterChecked by remember { mutableStateOf(false) }
    var marvelChecked by remember { mutableStateOf(false) }

    // Initial data fetch
    LaunchedEffect(Unit) {
        // TODO: Make this async when backend implements suspend functions
        itemList = ApiService.getMySets()
        isLoading = false
    }

    // Function to apply filters and search
    fun applyFiltersAndSearch() {
        isLoading = true

        // Build genre list
        val genres = mutableListOf<String>()
        if (starWarsChecked) genres.add("Star Wars")
        if (indianaJonesChecked) genres.add("Indiana Jones")
        if (harryPotterChecked) genres.add("Harry Potter")
        if (marvelChecked) genres.add("Marvel")

        // Convert price strings to doubles
        val minPrice = priceMin.toDoubleOrNull()
        val maxPrice = priceMax.toDoubleOrNull()

        // Call API with all filters
        // TODO: Make this async when backend implements suspend functions
        itemList = ApiService.searchMySets(
            searchQuery = activeSearchQuery,
            priceMin = minPrice,
            priceMax = maxPrice,
            dateAcquired = dateAcquired.takeIf { it.isNotBlank() },
            genres = genres
        )

        isLoading = false
        currentPage = 1 // Reset to first page after filtering
    }

    // Calculate pagination values
    val totalPages = remember(itemList, itemsPerPage) {
        ((itemList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    }

    // Reset to page 1 if current page exceeds total pages
    if (currentPage > totalPages) {
        currentPage = 1
    }

    // Get items for current page
    val paginatedList = remember(itemList, currentPage, itemsPerPage) {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(itemList.size)
        if (startIndex < itemList.size) {
            itemList.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        // Top Bar with Search Bar and Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by Name") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        activeSearchQuery = searchQuery
                        applyFiltersAndSearch()
                    }
                )
            )

            IconToggleButton(
                checked = showFilterWidget,
                onCheckedChange = { showFilterWidget = it },
                modifier = Modifier
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = if (showFilterWidget)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Toggle Filter",
                    tint = if (showFilterWidget)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // LazyColumn with containers
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Results Widget at the top (only when toggle is on)
            if (showFilterWidget) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Filter Options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )

                            HorizontalDivider()

                            // Price Min
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Price Min:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.4f)
                                )
                                OutlinedTextField(
                                    value = priceMin,
                                    onValueChange = { priceMin = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$0.00") },
                                    singleLine = true
                                )
                            }

                            // Price Max
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Price Max:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.4f)
                                )
                                OutlinedTextField(
                                    value = priceMax,
                                    onValueChange = { priceMax = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$999.99") },
                                    singleLine = true
                                )
                            }

                            // Date Acquired
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Date Acquired:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.4f)
                                )
                                OutlinedTextField(
                                    value = dateAcquired,
                                    onValueChange = { dateAcquired = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("MM/DD/YYYY") },
                                    singleLine = true
                                )
                            }

                            // Filler Field 1
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filler Field 1:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.4f)
                                )
                                OutlinedTextField(
                                    value = fillerField1,
                                    onValueChange = { fillerField1 = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("Enter value") },
                                    singleLine = true
                                )
                            }

                            // Filler Field 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filler Field 2:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(0.4f)
                                )
                                OutlinedTextField(
                                    value = fillerField2,
                                    onValueChange = { fillerField2 = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("Enter value") },
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Genres",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )

                            // Checkboxes for genres
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = starWarsChecked,
                                    onCheckedChange = { starWarsChecked = it }
                                )
                                Text(
                                    text = "Star Wars",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = indianaJonesChecked,
                                    onCheckedChange = { indianaJonesChecked = it }
                                )
                                Text(
                                    text = "Indiana Jones",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = harryPotterChecked,
                                    onCheckedChange = { harryPotterChecked = it }
                                )
                                Text(
                                    text = "Harry Potter",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = marvelChecked,
                                    onCheckedChange = { marvelChecked = it }
                                )
                                Text(
                                    text = "Marvel",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            // Apply Filters Button
                            Button(
                                onClick = { applyFiltersAndSearch() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Apply Filters")
                            }
                        }
                    }
                }
            }

            // Show loading or items
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...")
                    }
                }
            } else {
                items(paginatedList) { set ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSet = set },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Image on the left
                            AsyncImage(
                                model = set.link,
                                contentDescription = "LEGO Set Image",
                                modifier = Modifier.size(60.dp),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Set name
                            Text(
                                text = set.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )

                            // Price on the far right
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Current Price",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$${set.price}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Pagination Controls - scrollable at the bottom
            if (totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back arrow
                        IconButton(
                            onClick = { currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_media_previous),
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 1)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Page indicator
                        Text(
                            text = "Page $currentPage/$totalPages",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Forward arrow
                        IconButton(
                            onClick = { currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_media_next),
                                contentDescription = "Next Page",
                                tint = if (currentPage < totalPages)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Popup Dialog when a set is selected
    if (selectedSet != null) {
        Dialog(
            onDismissRequest = { selectedSet = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header with X button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Same row as the list item
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = getDrawableId(selectedSet!!.imageId)),
                                contentDescription = selectedSet!!.name,
                                modifier = Modifier.size(60.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = selectedSet!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Close button
                        IconButton(onClick = { selectedSet = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Filler content rows
                    Text("Filler Row 1", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filler Row 2", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filler Row 3", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filler Row 4", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

// Helper function to get drawable resource ID
private fun getDrawableId(imageNumber: Int): Int {
    return when (imageNumber) {
        1 -> R.drawable.image1
        2 -> R.drawable.image2
        3 -> R.drawable.image3
        4 -> R.drawable.image4
        else -> R.drawable.image1 // Default fallback
    }
}



