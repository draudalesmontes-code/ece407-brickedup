package com.cs407.brickcollector.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cs407.brickcollector.R
import com.cs407.brickcollector.api.LegoDatabase
import com.cs407.brickcollector.models.LegoSet
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserViewModel
import com.cs407.location.viewModels.LatlngToCity
import com.cs407.location.viewModels.callLocationVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BuyScreen(
    vm: callLocationVM,
    onNavigateToSettings: () -> Unit = {},
    userViewModel: UserViewModel
) {
    val userState by userViewModel.userState.collectAsState()
    var isLoading by remember { mutableStateOf(true) }
    var itemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var fullItemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var showFilterWidget by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<LegoSet?>(null) }
    var selectedCity by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val geoKey = remember { context.getString(R.string.geoapify_api_key) }

    var userCity by remember { mutableStateOf<String?>(null) }

    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }

    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val themeOptions = listOf("All", "Harry Potter", "Star Wars", "Marvel")
    var selectedThemeFilter by remember { mutableStateOf("All") }
    val userFirestore = UserFirestore()
    val legoPriceDb = remember { LegoDatabase.getInstance(context) }
    var dbPrices by remember { mutableStateOf<Map<Int, Double>>(emptyMap()) }
    var marketItems by remember { mutableStateOf<List<UserFirestore.MarketSellEntry>>(emptyList()) }

    LaunchedEffect(userState.uid) {
        if (userState.uid.isNotEmpty()) {
            isLoading = true
            // Pass the current user's UID so getBuyList can filter it out
            userFirestore.getBuyList(userState.uid) { entries ->
                // Items are already filtered in getBuyList
                marketItems = entries
                fullItemList = entries.map { it.set }.distinctBy { it.setId }
                itemList = fullItemList
                isLoading = false

                Log.d("BuyScreen", "Loaded ${entries.size} items for purchase")
            }
            val userLatLng = vm.fetchLatLngOnce()
            if (userLatLng != null) {
                val userCityVM = LatlngToCity()
                userCityVM.resolveAndStore(userLatLng, apiKey = geoKey)
                userCity = userCityVM.cityCounty.value
            }
        } else {
            isLoading = false
        }
    }
    LaunchedEffect(fullItemList) {
        if (fullItemList.isNotEmpty()) {
            val priceMap = mutableMapOf<Int, Double>()

            withContext(Dispatchers.IO) {
                fullItemList.forEach { set ->
                    try {
                        val dbSet = legoPriceDb.getSetById(set.setId)
                        val basePrice = dbSet?.newPrice ?: dbSet?.usedPrice
                        if (basePrice != null) {
                            priceMap[set.setId] = basePrice
                        }
                    } catch (e: Exception) {
                        Log.e("BuyScreen", "Error loading db price for ${set.setId}", e)
                    }
                }
            }

            dbPrices = priceMap
        } else {
            dbPrices = emptyMap()
        }
    }

    fun applyFiltersAndSearch() {
        var filtered = fullItemList

        if (activeSearchQuery.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(activeSearchQuery, ignoreCase = true) }
        }

        if (selectedThemeFilter != "All") {
            filtered = filtered.filter { set ->
                when (selectedThemeFilter) {
                    "Harry Potter" -> set.name.contains("Harry Potter", ignoreCase = true)
                    "Star Wars" -> set.name.contains("Star Wars", ignoreCase = true)
                    "Marvel" -> set.name.contains("Marvel", ignoreCase = true)
                    else -> true
                }
            }
        }


        priceMin.toDoubleOrNull()?.let { min ->
            filtered = filtered.filter { it.price >= min }
        }

        priceMax.toDoubleOrNull()?.let { max ->
            filtered = filtered.filter { it.price <= max }
        }

        itemList = filtered
        currentPage = 1
    }

    val combinedList = remember(itemList, marketItems, userCity) {
        val flat = mutableListOf<Pair<LegoSet, String?>>()

        for (set in itemList) {
            val sellersForSet = marketItems.filter { it.set.setId == set.setId }

            if (sellersForSet.isEmpty()) {
                flat.add(set to null)
            } else {
                sellersForSet.forEach { entry ->
                    flat.add(entry.set to entry.sellerCity)
                }
            }
        }

        if (userCity.isNullOrBlank()) {
            flat
        } else {
            val normalizedUserCity = userCity!!.trim().lowercase()

            flat.sortedByDescending { (_, sellerCity) ->
                val normalizedSellerCity = sellerCity?.trim()?.lowercase()
                normalizedSellerCity != null &&
                        (normalizedSellerCity == normalizedUserCity ||
                                normalizedSellerCity.contains(normalizedUserCity) ||
                                normalizedUserCity.contains(normalizedSellerCity))
            }
        }
    }

    val totalPages = remember(combinedList, itemsPerPage) {
        ((combinedList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    }

    if (currentPage > totalPages) {
        currentPage = 1
    }

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                    onValueChange = { input ->
                                        priceMin = input.filter { it.isDigit() || it == '.' }
                                    },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$0.00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Number)
                                )
                            }

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
                                    onValueChange = { input ->
                                        priceMax = input.filter { it.isDigit() || it == '.' }
                                    },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$999.99") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Number

                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                themeOptions.forEach { option ->
                                    Button(
                                        onClick = { selectedThemeFilter = option },
                                        modifier = Modifier.weight(1f),
                                        enabled = selectedThemeFilter != option
                                    ) {
                                        Text(option)
                                    }
                                }
                            }

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

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (itemList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No sets available for purchase",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(paginatedList) { index, pair ->
                    val (set, cityForCard) = pair

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSet = set
                                selectedCity = cityForCard},
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = set.imageUrl,
                                contentDescription = "LEGO Set Image",
                                modifier = Modifier.size(60.dp),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = set.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )

                            val (set, cityForCard) = pair
                            val basePrice = dbPrices[set.setId]

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Asking price: $${String.format("%.2f", set.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (basePrice != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Price: $${String.format("%.2f", basePrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (cityForCard != null)
                                        "Seller city: $cityForCard"
                                    else
                                        "Seller: No city",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                        }
                    }
                }
            }

            if (totalPages > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Text("<")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = "Page $currentPage/$totalPages",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Text(">")
                        }
                    }
                }
            }
        }
    }

    if (selectedSet != null) {
        Dialog(
            onDismissRequest = {  selectedSet = null
                selectedCity = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = selectedSet!!.imageUrl,
                                contentDescription = selectedSet!!.name,
                                modifier = Modifier.size(60.dp),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = selectedSet!!.name,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        IconButton(onClick = { selectedSet = null
                            selectedCity = null}) {
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

                    Text("Set ID: ${selectedSet!!.setId}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    val basePriceForDialog = dbPrices[selectedSet!!.setId]

                    Text("Asking Price: $${String.format("%.2f", selectedSet!!.price)}",
                        style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Database Price: " + (
                                basePriceForDialog?.let { "$" + String.format("%.2f", it) } ?: "N/A"
                                ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Seller city: ${selectedCity ?: "Not available"}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val currentSet = selectedSet
                            if (currentSet != null) {
                                // Try to find the matching seller entry for this set (and city if known)
                                val matchingEntries = marketItems.filter { it.set.setId == currentSet.setId }
                                val entry = if (selectedCity != null) {
                                    matchingEntries.firstOrNull { it.sellerCity == selectedCity }
                                } else {
                                    matchingEntries.firstOrNull()
                                }

                                val sellerUidForEntry = entry?.sellerUid

                                if (!sellerUidForEntry.isNullOrBlank()) {
                                    // Fetch email from Firestore, then copy to clipboard
                                    userFirestore.getEmail(sellerUidForEntry) { sellerEmail ->
                                        if (!sellerEmail.isNullOrBlank()) {
                                            clipboardManager.setText(AnnotatedString(sellerEmail))
                                            Toast.makeText(
                                                context,
                                                "Seller email copied to clipboard",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Seller email not available",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Seller email not found for this listing",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            }

                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Contact Seller")
                    }
                }
            }
        }
    }
}