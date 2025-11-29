package com.cs407.brickcollector
//43 , -89.4
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.brickcollector.api.LegoDatabase
import com.cs407.brickcollector.models.UserDatabase
import com.cs407.brickcollector.models.UserFirestore
import com.cs407.brickcollector.models.UserState
import com.cs407.brickcollector.models.UserViewModel
import com.cs407.brickcollector.ui.LoginPage
import com.cs407.brickcollector.ui.screens.BuyScreen
import com.cs407.brickcollector.ui.screens.MySetsScreen
import com.cs407.brickcollector.ui.screens.SellScreen
import com.cs407.brickcollector.ui.screens.SettingsScreen
import com.cs407.brickcollector.ui.screens.WantListScreen
import com.cs407.location.uiScreens.qrCameraScreen
import com.cs407.location.viewModels.callLocationVM
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    private val vm: callLocationVM by viewModels()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    object AppState {
        var cameraOn = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val geoKey = getString(R.string.geoapify_api_key)


        // Check if database file exists in assets
        try {
            val assetFiles = assets.list("")
            Log.d("LegoTest", "Files in assets: ${assetFiles?.joinToString()}")

            val hasDb = assetFiles?.contains("lego_sets.db") ?: false
            Log.d("LegoTest", "Database in assets: $hasDb")
        } catch (e: Exception) {
            Log.e("LegoTest", "Error checking assets: ${e.message}")
        }

        // Now try to access database
        val legoDb = LegoDatabase.getInstance(this)
        val set = legoDb.getSetByUPC("673419266192")
        Log.d("LegoTest", "Found: ${set?.name ?: "Not found"}")

        setContent {
            AppNavigation(vm)
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { perms ->
            val granted =
                perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

            vm.updatePermission(granted)

            if (granted) {
                // We have permission -> resolve city
                lifecycleScope.launch {
                    val city = vm.resolveCityAssumingPermission(
                        appContext = applicationContext,
                        geoapifyApiKey = geoKey
                    )

                    Log.d("CITY", "Resolved city: $city")
                    Toast.makeText(this@MainActivity, "City: $city", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Location permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // 4) Initialize VM with context + API key
        vm.initialize(applicationContext, geoKey)

        // 5) If no permission yet, ask. Otherwise go ahead and resolve.
        if (!vm.hasLocationPermission(this)) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            vm.updatePermission(true)
            lifecycleScope.launch {
                val city = vm.resolveCityAssumingPermission(
                    appContext = applicationContext,
                    geoapifyApiKey = geoKey
                )
                val Latlng = vm.fetchLatLngOnce()
                Toast.makeText(this@MainActivity, "Latlng: $Latlng", Toast.LENGTH_SHORT).show()
                Log.d("CITY", "Resolved city (already had perm): $city")
                Toast.makeText(this@MainActivity, "City: $city", Toast.LENGTH_SHORT).show()
            }
        }

    }
}

    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Call the example usage function
        lifecycleScope.launch {
            exampleUsage()
        }

        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Testing BrickEconomy API\nCheck Logcat for results",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

     */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(vm: callLocationVM, userViewModel: UserViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var currentUser by remember { mutableStateOf<UserState?>(null) }

    val bottomNavItems = listOf(
        BottomNavItem("my_sets", "My Sets", Icons.Default.Home),
        BottomNavItem("want_list", "Want List", Icons.AutoMirrored.Filled.List),
        BottomNavItem("buy", "Buy", Icons.Default.ShoppingCart),
        BottomNavItem("sell", "Sell", Icons.Default.Share)
    )

    val showBars = currentRoute != "settings"

    Scaffold(
        topBar = {
            when (currentRoute) {
                // Show no default top bar (shows top bar from SettingsScreen
                "settings" -> {}

                // Show default top bar
                else -> {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            when(currentRoute) {
                                "login" -> {}
                                else -> {
                                    IconButton(onClick = { navController.navigate("settings") }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                                    }
                                }
                            }

                        },
                        actions = {
                            IconButton(onClick = {
                                if (MainActivity.AppState.cameraOn) {
                                    // Camera is on, turn it off by going back
                                    MainActivity.AppState.cameraOn = false
                                    navController.popBackStack()
                                } else {
                                    // Camera is off, turn it on by navigating to scanner
                                    MainActivity.AppState.cameraOn = true
                                    navController.navigate("qrScanner")
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = "Scan Barcode"
                                )
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(navController = navController, items = bottomNavItems)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = /*"my_sets"*/ "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginPage (
                    modifier = Modifier,
                    loginButtonClick = { userState ->
                        currentUser = userState
                        userViewModel.setUser(userState)
                        navController.navigate("my_sets")}
                )
            }
            composable("my_sets") {
                MySetsScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("want_list") {
                WantListScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("buy") {
                BuyScreen(
                    vm = vm,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("sell") {
                SellScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val userFirestore = remember { UserFirestore() }
                val db = remember(context) { UserDatabase.getDatabase(context) }

                SettingsScreen(
                    currentUser = currentUser,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        // 1) Sign out from Firebase (stops auto-login)
                        FirebaseAuth.getInstance().signOut()

                        // 2) Clear the app-level user state
                        currentUser = null

                        // 3) Make sure camera is off (just in case)
                        MainActivity.AppState.cameraOn = false

                        // 4) Navigate to login and clear back stack
                        navController.navigate("login") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onDeleteAccount = {
                        val user = FirebaseAuth.getInstance().currentUser
                        if (user != null) {
                            val uid = user.uid
                            val id = currentUser?.id

                            scope.launch {

                                // Delete user data from Firestore
                                userFirestore.removeUser(uid)

                                // Delete user from room database
                                if (id != null) {
                                    withContext(Dispatchers.IO) {
                                        db.deleteDao().deleteUser(id)
                                    }
                                }

                                user.delete()
                                    .addOnSuccessListener {
                                        Log.d(
                                            "DeleteAccount",
                                            "Firebase Auth user deleted successfully."
                                        )
                                    }

                                // Show success message
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Account successfully deleted.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                // Navigate to login and clear the back stack
                                navController.navigate("login") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                )
            }
            composable("qrScanner") {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                qrCameraScreen { scannedValue ->

                    scope.launch {
                        // 1) Get DB instance
                        val legoDb = LegoDatabase.getInstance(context)

                        // 2) Query DB on background thread
                        val set = withContext(Dispatchers.IO) {
                            legoDb.getSetByUPC(scannedValue)  // uses the UPC column
                        }

                        // 3) Build a message to show
                        val msg = if (set != null) {
                            "UPC: $scannedValue\nSet: ${set.setNumber} - ${set.name}"
                        } else {
                            "No set found for UPC $scannedValue"
                        }

                        // 4) Log + Toast for testing
                        Log.d("QR-Lego", msg)
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

                        // 5) Go back to previous screen
                        MainActivity.AppState.cameraOn = false
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    // If camera is on, turn it off and pop back first
                    if (MainActivity.AppState.cameraOn) {
                        MainActivity.AppState.cameraOn = false
                        navController.popBackStack()
                    }

                    // Then navigate to the selected item
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }

            )
        }
    }
}