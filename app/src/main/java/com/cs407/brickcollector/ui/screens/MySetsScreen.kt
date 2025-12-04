package com.cs407.brickcollector.ui.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.brickcollector.models.LegoSet
import com.cs407.brickcollector.models.ListsViewModel
import com.cs407.brickcollector.models.UserDatabase
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MySetsScreen(
    onNavigateToSettings: () -> Unit = {},
    userViewModel: UserViewModel
) {
    val listsViewModel: ListsViewModel = viewModel()
    val userState by userViewModel.userState.collectAsState()

    val context = LocalContext.current
    val userDatabase = remember { UserDatabase.getDatabase(context) }
    val userFirestore = remember { UserFirestore() }
    val coroutineScope = rememberCoroutineScope()

    var itemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var fullItemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var showFilterWidget by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<LegoSet?>(null) }

    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }

    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    val themeOptions = listOf("All", "Harry Potter", "Star Wars", "Marvel")
    var selectedThemeFilter by remember { mutableStateOf("All") }
    // Load My Sets from Firestore - only once
    LaunchedEffect(userState.uid) {
        if (userState.uid.isNotEmpty()) {
            isLoading = true
            userFirestore.getSetsFromMyList(userState.uid) { sets ->
                val loadedSets = sets ?: emptyList()
                fullItemList = loadedSets
                itemList = loadedSets
                isLoading = false
                android.util.Log.d("MySetsScreen", "Loaded ${loadedSets.size} sets from Firestore")
            }
        } else {
            isLoading = false
        }
    }
    fun formatPriceInput(input: String): String {
        val clean = input.replace("$", "").trim()
        return if (clean.isBlank()) "" else "$$clean"
    }
    fun parsePrice(input: String): Double? {
        val clean = input.replace("$", "").trim()
        return clean.toDoubleOrNull()
    }



    fun applyFiltersAndSearch() {
        // Always start from the full list
        var filtered = fullItemList

        // Text search by name
        if (activeSearchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(activeSearchQuery, ignoreCase = true)
            }
        }

        // Theme filter based on set name (Room LegoSet doesn't have theme field)
        if (selectedThemeFilter != "All") {
            filtered = filtered.filter { set ->
                when (selectedThemeFilter) {
                    "Harry Potter" -> set.name.contains("Harry Potter", ignoreCase = true)
                    "Star Wars"    -> set.name.contains("Star Wars", ignoreCase = true)
                    "Marvel"       -> set.name.contains("Marvel", ignoreCase = true)
                    else           -> true
                }
            }
        }

        // Numeric price filtering using parsed Doubles
        parsePrice(priceMin)?.let { min ->
            filtered = filtered.filter { it.price >= min }
        }

        parsePrice(priceMax)?.let { max ->
            filtered = filtered.filter { it.price <= max }
        }

        itemList = filtered
        currentPage = 1
    }

    val totalPages = remember(itemList, itemsPerPage) {
        ((itemList.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    }

    if (currentPage > totalPages) {
        currentPage = 1
    }

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
                                    onValueChange = { input -> priceMin = formatPriceInput(input) },
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
                                    onValueChange = { input -> priceMax = formatPriceInput(input) },                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$999.99") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Number)
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
                            "No sets in My Sets",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

    // Set Details Dialog
    if (selectedSet != null) {
        Dialog(
            onDismissRequest = { selectedSet = null },
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

                    Text("Set ID: ${selectedSet!!.setId}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Price: $${selectedSet!!.price}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val setToMove = selectedSet
                                if (setToMove != null && userState.id != 0) {
                                    coroutineScope.launch {
                                        // Add to Want List
                                        withContext(Dispatchers.IO) {
                                            userDatabase.legoDao().insertWantListSet(userState.id, setToMove)
                                        }
                                        userFirestore.addSetToWantList(userState.uid, setToMove)

                                        // Remove from My Sets
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromMyList(listOf(setToMove.setId), userState.id)
                                        }
                                        userFirestore.removeSetFromMyList(userState.uid, setToMove)

                                        // Update UI
                                        fullItemList = fullItemList.filter { it.setId != setToMove.setId }
                                        itemList = itemList.filter { it.setId != setToMove.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Move to Want List")
                        }

                        Button(
                            onClick = {
                                val setToMove = selectedSet
                                if (setToMove != null && userState.id != 0) {
                                    coroutineScope.launch {
                                        // Add to Sell List
                                        withContext(Dispatchers.IO) {
                                            userDatabase.legoDao().insertSellListSet(userState.id, setToMove)
                                        }
                                        userFirestore.addSetToSellList(userState.uid, setToMove)

                                        // Remove from My Sets
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromMyList(listOf(setToMove.setId), userState.id)
                                        }
                                        userFirestore.removeSetFromMyList(userState.uid, setToMove)

                                        // Update UI
                                        fullItemList = fullItemList.filter { it.setId != setToMove.setId }
                                        itemList = itemList.filter { it.setId != setToMove.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Move to Sell List")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val setToRemove = selectedSet
                            if (setToRemove != null && userState.id != 0) {
                                coroutineScope.launch {
                                    // Remove from Room database (MY_LIST specifically)
                                    withContext(Dispatchers.IO) {
                                        userDatabase.deleteDao().deleteFromMyList(listOf(setToRemove.setId), userState.id)
                                    }

                                    // Remove from Firestore
                                    userFirestore.removeSetFromMyList(userState.uid, setToRemove)

                                    // Update both filtered and full lists
                                    fullItemList = fullItemList.filter { it.setId != setToRemove.setId }
                                    itemList = itemList.filter { it.setId != setToRemove.setId }

                                    selectedSet = null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove from My Sets")
                    }
                }
            }
        }
    }
}