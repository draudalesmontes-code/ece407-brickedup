package com.cs407.brickcollector.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cs407.brickcollector.R
import com.cs407.brickcollector.api.ApiService
import com.cs407.brickcollector.models.UserFirestore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import com.cs407.brickcollector.models.UserState
import com.cs407.location.viewModels.callLocationVM


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUser: UserState?,
    vm: callLocationVM,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var city by remember { mutableStateOf("") }
    // User profile state
    var username by remember { mutableStateOf("") }
    var setsOwned by remember { mutableStateOf("0") }
    var setsSold by remember { mutableStateOf("0") }
    var totalEarned by remember { mutableStateOf("$0.00") }

    // Temporary state for editing
    var editedUsername by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val userFirestore = remember { UserFirestore() }
    var editedCity by remember { mutableStateOf("") }
    val context = LocalContext.current

    var showCityDialog by remember { mutableStateOf(false) }
    var detectedCity by remember { mutableStateOf<String?>(null) }
    var hasAskedForCity by remember { mutableStateOf(false) }
    // Fetch user profile and stats on load
    LaunchedEffect(currentUser?.uid) {
        val firebaseUser = Firebase.auth.currentUser

        if (firebaseUser == null) {
            username = "Guest"
            editedUsername = username
            isLoading = false
        } else {
            username = firebaseUser.displayName.toString()
            editedUsername = username

            // Load user statistics
            if (currentUser?.uid?.isNotEmpty() == true) {
                userFirestore.getUserStats(currentUser.uid) { stats ->
                    if (stats != null) {
                        setsOwned = stats.setsOwned.toString()
                        setsSold = stats.setsSold.toString()
                        totalEarned = String.format("$%.2f", stats.totalEarned)
                    }
                    isLoading = false
                }
                userFirestore.getCity(currentUser.uid) { storedCity ->
                    val cleaned = if (storedCity == null || storedCity == "none") "" else storedCity
                    city = cleaned
                    editedCity = cleaned
                }
            } else {
                isLoading = false
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar with Back Arrow and Edit Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.vert_description)
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit",
                                tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        text =  { Text(if (isEditing) "Save" else "Edit") },
                        onClick = {
                            if (isEditing) {
                                // Save changes
                                val updated = ApiService.updateUserProfile(editedUsername)
                                val newCityForStorage = editedCity.ifBlank { "none" }

                                if (currentUser?.uid?.isNotEmpty() == true) {
                                    userFirestore.updateCity(currentUser.uid, newCityForStorage)
                                }

                                if (updated) {
                                    username = editedUsername
                                    // Keep UI pretty: hide "none" and show blank instead
                                    city = if (newCityForStorage == "none") "" else newCityForStorage
                                    isEditing = false
                                }

                            } else {
                                editedUsername = username
                                editedCity = city
                                hasAskedForCity = false
                                isEditing = true
                            }
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = stringResource(R.string.logout_description)
                            )
                        },
                        text = { Text(stringResource(R.string.logout_button)) },
                        onClick = {
                            Firebase.auth.signOut()
                            expanded = false
                            onLogout()
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.del_acc_description)
                            )
                        },
                        text = { Text(stringResource(R.string.del_acc_button)) },
                        onClick = {
                            expanded = false
                            showDeleteConfirmDialog = true
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = Color.Red,
                            leadingIconColor = Color.Red
                        )
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section - Only Username and Profile Pic
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = username.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Username only
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editedUsername,
                                    onValueChange = { editedUsername = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editedCity,
                                    onValueChange = { newValue ->
                                        val wasBlank = editedCity.isBlank()
                                        editedCity = newValue

                                        // First time they type into a blank city -> offer current location
                                        if (wasBlank && newValue.isNotBlank() && !hasAskedForCity) {
                                            hasAskedForCity = true
                                            scope.launch {
                                                val geoKey = context.getString(R.string.geoapify_api_key)
                                                val currentCity = vm.resolveCityAssumingPermission(
                                                    appContext = context.applicationContext,
                                                    geoapifyApiKey = geoKey
                                                )
                                                if (!currentCity.isNullOrBlank()) {
                                                    detectedCity = currentCity
                                                    showCityDialog = true
                                                }
                                            }
                                        }
                                    },
                                    label = { Text("City / Location") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "City: ${if (city.isBlank()) "None" else city}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Statistics Cards
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = setsOwned,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sets Owned",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = setsSold,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sets Sold",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                alpha = 0.3f
                            )
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = totalEarned,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total Earned",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
    if (showCityDialog && !detectedCity.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Use current city?") },
            text = {
                Text("We detected your current city as \"${detectedCity}\". Do you want to use this for your profile?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Autofill and close
                        editedCity = detectedCity ?: editedCity
                        city = detectedCity ?: city
                        showCityDialog = false
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Let the user keep what they typed
                        showCityDialog = false
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
            },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
            },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to permanently delete your account? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}