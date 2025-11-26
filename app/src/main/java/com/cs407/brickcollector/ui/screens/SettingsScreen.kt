package com.cs407.brickcollector.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cs407.brickcollector.api.ApiService
import com.cs407.brickcollector.models.UserState

@Composable
fun SettingsScreen(
    currentUser: UserState?,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // User profile state
    var username by remember { mutableStateOf("") }
    var setsOwned by remember { mutableStateOf("") }
    var setsListed by remember { mutableStateOf("") }

    // Temporary state for editing
    var editedUsername by remember { mutableStateOf("") }

    // Fetch user profile on load
    LaunchedEffect(currentUser) {
        // TODO: Make this async when backend implements suspend functions

      //  val profile = ApiService.getUserProfile()
        username = currentUser?.username ?: "Guest"
        setsOwned =  "0"  //profile.setsOwned
        setsListed = "0"   // profile.setsListed
        editedUsername = username
        isLoading = false
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

            IconButton(onClick = {
                if (isEditing) {
                    // Save changes
                    if (ApiService.updateUserProfile(editedUsername)) {
                        username = editedUsername
                        isEditing = false
                    }
                } else {
                    // Enter edit mode
                    isEditing = true
                }
            }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Edit",
                    tint = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Section
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

                        // Username and Stats Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Username
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editedUsername,
                                    onValueChange = { editedUsername = it },
                                    label = { Text("Username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = username,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Sets Owned
                                Column {
                                    Text(
                                        text = setsOwned,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Sets Owned",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Sets Listed
                                Column {
                                    Text(
                                        text = setsListed,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Listed to Sell",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Settings Cards
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 1",
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
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 2",
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
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 3",
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
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 4",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Settings Cards (duplicated as per original design)
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 1",
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
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 2",
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
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 3",
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
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Card 4",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Bottom Buttons inside LazyColumn
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                ApiService.switchAccount()
                                // TODO: Handle navigation after switch
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch Account")
                        }

                        Button(
                            onClick = {
                                ApiService.signOut()
                                onLogout()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Sign Out")
                        }
                    }
                }
            }
        }
    }
}