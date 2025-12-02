package com.cs407.brickcollector.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.brickcollector.models.LegoSet
import com.cs407.brickcollector.models.UserDatabase
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SellScreen(
    onNavigateToSettings: () -> Unit = {},
    userViewModel: UserViewModel = viewModel()
) {
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

    var showAddDialog by remember { mutableStateOf(false) }
    var addSearchQuery by remember { mutableStateOf("") }
    var mySetsItemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var showConfirmSellDialog by remember { mutableStateOf(false) }

    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }

    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }

    // Load Sell List and My Sets from Firestore - only once
    LaunchedEffect(userState.uid) {
        if (userState.uid.isNotEmpty()) {
            isLoading = true

            // Load Sell List - THIS USER'S sell list only
            userFirestore.getSetsFromSellList(userState.uid) { sets ->
                val loadedSets = sets ?: emptyList()
                fullItemList = loadedSets
                itemList = loadedSets
                isLoading = false
                Log.d("SellScreen", "Loaded ${loadedSets.size} sets in sell list")
            }

            // Also load My Sets for the add dialog
            userFirestore.getSetsFromMyList(userState.uid) { sets ->
                mySetsItemList = sets ?: emptyList()
                Log.d("SellScreen", "Loaded ${mySetsItemList.size} sets in my sets")
            }
        } else {
            isLoading = false
        }
    }

    fun applyFiltersAndSearch() {
        var results = fullItemList

        if (activeSearchQuery.isNotBlank()) {
            results = results.filter { it.name.contains(activeSearchQuery, ignoreCase = true) }
        }

        priceMin.toDoubleOrNull()?.let { min ->
            results = results.filter { it.price >= min }
        }

        priceMax.toDoubleOrNull()?.let { max ->
            results = results.filter { it.price <= max }
        }

        itemList = results
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

            IconButton(
                onClick = {
                    showAddDialog = true
                    addSearchQuery = ""
                },
                modifier = Modifier
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Set to Sell",
                    tint = MaterialTheme.colorScheme.onSurface
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
                                    onValueChange = { priceMin = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$0.00") },
                                    singleLine = true
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
                                    onValueChange = { priceMax = it },
                                    modifier = Modifier.weight(0.6f),
                                    placeholder = { Text("$999.99") },
                                    singleLine = true
                                )
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
                            "No sets listed for sale",
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
                                contentDescription = set.name,
                                modifier = Modifier.size(80.dp),
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
                                    text = "Asking Price",
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
                    .fillMaxHeight(0.5f)
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

                    Text("Asking Price: $${selectedSet!!.price}", style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val setToSell = selectedSet
                                if (setToSell != null && userState.id != 0) {
                                    coroutineScope.launch {
                                        // Remove from Room database (SELL_LIST, MY_LIST, and WANT_LIST)
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromSellList(listOf(setToSell.setId), userState.id)
                                            userDatabase.deleteDao().deleteFromMyList(listOf(setToSell.setId), userState.id)
                                            userDatabase.deleteDao().deleteFromWantList(listOf(setToSell.setId), userState.id)
                                        }

                                        // Remove from Firestore (all lists)
                                        userFirestore.removeSetFromSellList(userState.uid, setToSell)
                                        userFirestore.removeSetFromMyList(userState.uid, setToSell)
                                        userFirestore.removeSetFromWantList(userState.uid, setToSell)

                                        // Record the sale (increment setsSold and totalEarned)
                                        userFirestore.recordSale(userState.uid, setToSell.price)

                                        // Update UI
                                        fullItemList = fullItemList.filter { it.setId != setToSell.setId }
                                        itemList = itemList.filter { it.setId != setToSell.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Confirm Sell")
                        }

                        Button(
                            onClick = {
                                val setToMove = selectedSet
                                if (setToMove != null && userState.id != 0) {
                                    coroutineScope.launch {
                                        // Add back to My Sets
                                        withContext(Dispatchers.IO) {
                                            userDatabase.legoDao().insertMyListSet(userState.id, setToMove)
                                        }
                                        userFirestore.addSetToMyList(userState.uid, setToMove)

                                        // Remove from Sell List
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromSellList(listOf(setToMove.setId), userState.id)
                                        }
                                        userFirestore.removeSetFromSellList(userState.uid, setToMove)

                                        // Update UI
                                        fullItemList = fullItemList.filter { it.setId != setToMove.setId }
                                        itemList = itemList.filter { it.setId != setToMove.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Remove from Sell")
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Selling
    if (showConfirmSellDialog && selectedSet != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmSellDialog = false },
            title = { Text("Confirm Sale") },
            text = {
                Text("Are you sure you want to mark \"${selectedSet!!.name}\" as sold? This will permanently remove it from all your lists.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val setToSell = selectedSet
                        if (setToSell != null && userState.id != 0) {
                            coroutineScope.launch {
                                // Remove from Room database (SELL_LIST, MY_LIST, and WANT_LIST)
                                withContext(Dispatchers.IO) {
                                    userDatabase.deleteDao().deleteFromSellList(listOf(setToSell.setId), userState.id)
                                    userDatabase.deleteDao().deleteFromMyList(listOf(setToSell.setId), userState.id)
                                    userDatabase.deleteDao().deleteFromWantList(listOf(setToSell.setId), userState.id)
                                }

                                // Remove from Firestore (all lists)
                                userFirestore.removeSetFromSellList(userState.uid, setToSell)
                                userFirestore.removeSetFromMyList(userState.uid, setToSell)
                                userFirestore.removeSetFromWantList(userState.uid, setToSell)

                                // Update UI
                                fullItemList = fullItemList.filter { it.setId != setToSell.setId }
                                itemList = itemList.filter { it.setId != setToSell.setId }

                                showConfirmSellDialog = false
                                selectedSet = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Yes, Mark as Sold")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showConfirmSellDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Set Dialog - Shows My Sets
    if (showAddDialog) {
        // Log for debugging
        LaunchedEffect(mySetsItemList) {
            Log.d("SellScreen", "My Sets count in dialog: ${mySetsItemList.size}")
            mySetsItemList.forEach { set ->
                Log.d("SellScreen", "My Set: ${set.name} (ID: ${set.setId})")
            }
        }

        Dialog(
            onDismissRequest = {
                showAddDialog = false
                addSearchQuery = ""
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Set to List for Sale",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = {
                            showAddDialog = false
                            addSearchQuery = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = addSearchQuery,
                        onValueChange = { addSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search your sets...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    val filteredMySets = remember(mySetsItemList, addSearchQuery) {
                        if (addSearchQuery.isBlank()) {
                            mySetsItemList
                        } else {
                            mySetsItemList.filter {
                                it.name.contains(addSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    Text(
                        text = "Found ${mySetsItemList.size} sets in My Sets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (mySetsItemList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No sets in My Sets",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Add sets to My Sets first before listing them for sale",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (filteredMySets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No sets match your search",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredMySets) { set ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            // Add to sell list AND remove from my list
                                            coroutineScope.launch {
                                                // Add to Sell List
                                                withContext(Dispatchers.IO) {
                                                    userDatabase.legoDao().insertSellListSet(userState.id, set)
                                                }
                                                userFirestore.addSetToSellList(userState.uid, set)

                                                // Remove from My List
                                                withContext(Dispatchers.IO) {
                                                    userDatabase.deleteDao().deleteFromMyList(listOf(set.setId), userState.id)
                                                }
                                                userFirestore.removeSetFromMyList(userState.uid, set)

                                                // Update sell list UI
                                                fullItemList = fullItemList + set
                                                itemList = itemList + set

                                                // Update my sets list UI
                                                mySetsItemList = mySetsItemList.filter { it.setId != set.setId }

                                                showAddDialog = false
                                                addSearchQuery = ""
                                            }
                                        },
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
                                            contentDescription = set.name,
                                            modifier = Modifier.size(60.dp),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = set.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Set ID: ${set.setId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
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
                    }
                }
            }
        }
    }
}