package com.cs407.brickcollector.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.brickcollector.api.LegoDatabase
import com.cs407.brickcollector.models.LegoSet
import com.cs407.brickcollector.models.UserDatabase
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WantListScreen(
    onNavigateToSettings: () -> Unit = {},
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userState by userViewModel.userState.collectAsState()

    // Databases
    val legoDatabase = remember { LegoDatabase.getInstance(context) }
    val userDatabase = remember { UserDatabase.getDatabase(context) }
    val userFirestore = remember { UserFirestore() }

    var itemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("") }
    var showFilterWidget by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<LegoSet?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addSearchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<com.cs407.brickcollector.api.LegoSet>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var allSqlSets by remember {
        mutableStateOf<List<com.cs407.brickcollector.api.LegoSet>>(emptyList())
    }
    var fullItemList by remember { mutableStateOf<List<LegoSet>>(emptyList()) }
    val itemsPerPage = 7
    var currentPage by remember { mutableStateOf(1) }

    var priceMin by remember { mutableStateOf("") }
    var priceMax by remember { mutableStateOf("") }
    val themeOptions = listOf("All", "Harry Potter", "Star Wars", "Marvel")
    var selectedThemeFilter by remember { mutableStateOf("All") }

    // Load want list from Firestore
    LaunchedEffect(userState.uid) {
        if (userState.uid.isNotEmpty()) {
            userFirestore.getSetsFromWantList(userState.uid) { sets ->
                fullItemList = sets ?: emptyList()
                itemList = fullItemList
                isLoading = false
            }
        }
    }


    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            isSearching = true
            val results = withContext(Dispatchers.IO) {
                legoDatabase.getAllSets()
            }
            allSqlSets = results
            searchResults = results   // start by showing everything
            isSearching = false
        }
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

            IconButton(
                onClick = {
                    showAddDialog = true
                    addSearchQuery = ""
                    searchResults = emptyList()
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
                    contentDescription = "Add Set",
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
                                fontWeight = FontWeight.Bold
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

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val setToMove = selectedSet
                                if (setToMove != null && userState.id != 0) {
                                    scope.launch {
                                        android.util.Log.d("WantListScreen", "Moving set to My Sets: ${setToMove.name} (ID: ${setToMove.setId})")

                                        // Add to My Sets in Room
                                        withContext(Dispatchers.IO) {
                                            userDatabase.legoDao().insertMyListSet(userState.id, setToMove)
                                        }
                                        android.util.Log.d("WantListScreen", "Added to Room My Sets")

                                        // Add to My Sets in Firestore
                                        userFirestore.addSetToMyList(userState.uid, setToMove)
                                        android.util.Log.d("WantListScreen", "Added to Firestore My Sets")

                                        // Remove from Want List in Room
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromWantList(listOf(setToMove.setId), userState.id)
                                        }
                                        android.util.Log.d("WantListScreen", "Removed from Room Want List")

                                        // Remove from Want List in Firestore
                                        userFirestore.removeSetFromWantList(userState.uid, setToMove)
                                        android.util.Log.d("WantListScreen", "Removed from Firestore Want List")

                                        // Update UI
                                        itemList = itemList.filter { it.setId != setToMove.setId }
                                        fullItemList = fullItemList.filter { it.setId != setToMove.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Move to My Sets")
                        }

                        Button(
                            onClick = {
                                val setToRemove = selectedSet
                                if (setToRemove != null && userState.id != 0) {
                                    scope.launch {
                                        // Remove from Room database (WANT_LIST specifically)
                                        withContext(Dispatchers.IO) {
                                            userDatabase.deleteDao().deleteFromWantList(listOf(setToRemove.setId), userState.id)
                                        }
                                        // Remove from Firestore
                                        userFirestore.removeSetFromWantList(userState.uid, setToRemove)

                                        // Update UI
                                        itemList = itemList.filter { it.setId != setToRemove.setId }

                                        selectedSet = null
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }

    // Add Set Dialog with SQL Search
    if (showAddDialog) {
        Dialog(
            onDismissRequest = {
                showAddDialog = false
                addSearchQuery = ""
                searchResults = emptyList()
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
                            text = "Search LEGO Sets",
                            style = MaterialTheme.typography.titleLarge
                        )

                        IconButton(onClick = {
                            showAddDialog = false
                            addSearchQuery = ""
                            searchResults = emptyList()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = addSearchQuery,
                            onValueChange = { query ->
                                addSearchQuery = query

                                searchResults = if (query.isBlank()) {
                                    allSqlSets
                                } else {
                                    val q = query.trim()
                                    allSqlSets.filter { sqlSet ->
                                        sqlSet.name.contains(q, ignoreCase = true) ||
                                                sqlSet.setNumber.contains(q, ignoreCase = true)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Enter set name or number") },
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (addSearchQuery.isNotBlank()) {
                                    isSearching = true
                                    scope.launch {
                                        val results = withContext(Dispatchers.IO) {
                                            legoDatabase.searchSetsByName(addSearchQuery)
                                        }
                                        searchResults = results
                                        isSearching = false
                                    }
                                }
                            },
                            enabled = !isSearching && addSearchQuery.isNotBlank()
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Search")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (searchResults.isEmpty() && !isSearching) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (addSearchQuery.isBlank())
                                    "Enter a search query"
                                else
                                    "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults) { sqlSet ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newSet = LegoSet(
                                                name = sqlSet.name,
                                                setId = sqlSet.setId,
                                                price = sqlSet.newPrice ?: sqlSet.usedPrice ?: 0.0,
                                                imageUrl = sqlSet.imageUrl
                                            )

                                            scope.launch {
                                                // Add to Want List in Room
                                                withContext(Dispatchers.IO) {
                                                    userDatabase.legoDao().insertWantListSet(userState.id, newSet)
                                                }
                                                // Add to Want List in Firestore
                                                userFirestore.addSetToWantList(userState.uid, newSet)

                                                // Update UI
                                                itemList = itemList + newSet
                                                fullItemList = fullItemList + newSet

                                                android.util.Log.d("WantListScreen", "Added set: ${newSet.name} (ID: ${newSet.setId})")

                                                showAddDialog = false
                                                addSearchQuery = ""
                                                searchResults = emptyList()
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
                                            model = sqlSet.imageUrl ?: sqlSet.thumbnailUrl,
                                            contentDescription = sqlSet.name,
                                            modifier = Modifier.size(60.dp),
                                            contentScale = ContentScale.Crop
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sqlSet.name,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = "Set #${sqlSet.setNumber} | ${sqlSet.year}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${sqlSet.pieces} pieces",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val displayPrice = when {
                                                sqlSet.newPrice != null -> sqlSet.newPrice
                                                sqlSet.usedPrice != null -> sqlSet.usedPrice
                                                else -> 0.0
                                            }

                                                Text(
                                                    text = "$${"%.2f".format(displayPrice)}",
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
