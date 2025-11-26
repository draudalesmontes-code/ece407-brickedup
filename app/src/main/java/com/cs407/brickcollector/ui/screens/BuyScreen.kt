package com.cs407.brickcollector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cs407.brickcollector.R
import com.cs407.brickcollector.api.ApiService
import com.cs407.brickcollector.models.LegoSet
import com.cs407.location.viewModels.LatlngToCity
import com.cs407.location.viewModels.callLocationVM
import com.google.android.gms.maps.model.LatLng

@Composable
fun BuyScreen(
    vm: callLocationVM,
    onNavigateToSettings: () -> Unit = {}
) {

    // State for the list of sets available for purchase - fetched from API
    var itemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var showFilterWidget by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<LegoSet?>(null) }
    val context = LocalContext.current
    val geoKey = remember { context.getString(R.string.geoapify_api_key) }

    var userCity by remember { mutableStateOf<String?>(null) }

    // Pagination variables
    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }
    var cardCities by remember { mutableStateOf<List<String?>>(emptyList()) }
    // Filter state variables
    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    var starWarsChecked by remember { mutableStateOf(false) }
    var indianaJonesChecked by remember { mutableStateOf(false) }
    var harryPotterChecked by remember { mutableStateOf(false) }
    var marvelChecked by remember { mutableStateOf(false) }

    //hardcoded test for future buy
    //TODO: Remove once user database working hardcoded for now look at changes
    LaunchedEffect(Unit) {
        val allSets = ApiService.getAvailableForPurchase()
        itemList = allSets.take(4)
        val sellerLocation = listOf(
            LatLng(25.779460, -80.207658),//miami
            LatLng(43.072083, -89.408118),//madison
            LatLng(37.327717, -121.889255),//cali
        )
        val cityVM = LatlngToCity()

        val resolvedCities = mutableListOf<String?>()

        for (coord in sellerLocation) {
            cityVM.resolveAndStore(coord, apiKey = geoKey)


            resolvedCities.add(cityVM.cityCounty.value)  // may be null if it fails
        }

        cardCities = resolvedCities

        val userLatLng = vm.fetchLatLngOnce()
        if (userLatLng != null) {
            val userCityVM = LatlngToCity()
            userCityVM.resolveAndStore(userLatLng, apiKey = geoKey)
            userCity = userCityVM.cityCounty.value
        }

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
        itemList = ApiService.searchAvailableForPurchase(
            searchQuery = activeSearchQuery,
            priceMin = minPrice,
            priceMax = maxPrice,
            genres = genres
        )

        isLoading = false
        currentPage = 1 // Reset to first page after filtering
    }
    val combinedList = remember(itemList, cardCities, userCity) {
        val pairs = itemList.zip(cardCities)  // assumes same size

        if (userCity == null) {
            pairs
        } else {
            pairs.sortedByDescending { (_, sellerCity) ->
                sellerCity != null && sellerCity.equals(userCity, ignoreCase = true)
            }
        }
    }
    // Calculate pagination values
    val totalPages = remember(combinedList, itemsPerPage) {
        ((combinedList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    }

    // Reset to page 1 if current page exceeds total pages
    if (currentPage > totalPages) {
        currentPage = 1
    }

    // Get items for current page
    val paginatedList = remember(combinedList, currentPage, itemsPerPage) {
        val startIndex = (currentPage - 1) * itemsPerPage
        val endIndex = (startIndex + itemsPerPage).coerceAtMost(combinedList.size)
        if (startIndex < combinedList.size) {
            combinedList.subList(startIndex, endIndex)
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
                itemsIndexed(paginatedList) { index,pair ->
                    val (set, cityForCard) = pair

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
                                model = set.imageId,
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
                                    text = "Asking Price",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (cityForCard != null)
                                        "Seller city:  $cityForCard"
                                    else
                                        "Seller: No city",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(4.dp))

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

                    // Seller information
                    Text("Seller: User123", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Condition: New in Box", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Location: Madison, WI", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Asking Price: $${selectedSet!!.price}", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contact Seller Button
                    Button(
                        onClick = { /* TODO: Contact seller logic */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Contact Seller")
                    }
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