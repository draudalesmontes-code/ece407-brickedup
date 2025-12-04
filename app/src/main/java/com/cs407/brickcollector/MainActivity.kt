package com.cs407.brickcollector

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
import androidx.compose.runtime.collectAsState
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

        // Initialize database
        try {
            val assetFiles = assets.list("")
            Log.d("LegoTest", "Files in assets: ${assetFiles?.joinToString()}")

            val hasDb = assetFiles?.contains("lego_sets.db") ?: false
            Log.d("LegoTest", "Database in assets: $hasDb")
        } catch (e: Exception) {
            Log.e("LegoTest", "Error checking assets: ${e.message}")
        }

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
                lifecycleScope.launch {
                    val city = vm.resolveCityAssumingPermission(
                        appContext = applicationContext,
                        geoapifyApiKey = geoKey
                    )

                    Log.d("CITY", "Resolved city: $city")
                    //Toast.makeText(this@MainActivity, "City: $city", Toast.LENGTH_SHORT).show()
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
                //Toast.makeText(this@MainActivity, "Latlng: $Latlng", Toast.LENGTH_SHORT).show()
                Log.d("CITY", "Resolved city (already had perm): $city")
                //Toast.makeText(this@MainActivity, "City: $city", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

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

    // Only show navigation bars when logged in and not on login/settings
    val showBars = currentRoute != "settings" && currentRoute != "login"

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
                                    val targetRouteForScanner = when (currentRoute) {
                                        "want_list" -> "want_list"
                                        "sell" -> "sell"
                                        else -> "my_sets" // default: My Sets
                                    }
                                    MainActivity.AppState.cameraOn = true
                                    navController.navigate("qrScanner/$targetRouteForScanner")
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
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("login") {
                LoginPage(
                    modifier = Modifier,
                    loginButtonClick = { userState ->
                        currentUser = userState
                        userViewModel.setUser(userState)
                        navController.navigate("my_sets") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    vm = vm
                )
            }
            composable("my_sets") {

                MySetsScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    userViewModel = userViewModel
                )
            }
            composable("want_list") {
                WantListScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    userViewModel = userViewModel
                )
            }
            composable("buy") {
                BuyScreen(
                    vm = vm,
                    onNavigateToSettings = { navController.navigate("settings") },
                    userViewModel = userViewModel
                )
            }
            composable("sell") {
                SellScreen(
                    onNavigateToSettings = { navController.navigate("settings") },
                    userViewModel = userViewModel
                )
            }
            composable("settings") {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val userFirestore = remember { UserFirestore() }
                val db = remember(context) { UserDatabase.getDatabase(context) }

                SettingsScreen(
                    currentUser = currentUser,
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        currentUser = null
                        MainActivity.AppState.cameraOn = false
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
                                userFirestore.removeUser(uid)

                                if (id != null) {
                                    withContext(Dispatchers.IO) {
                                        db.deleteDao().deleteUser(id)
                                    }
                                }

                                user.delete()
                                    .addOnSuccessListener {
                                        Log.d("DeleteAccount", "Firebase Auth user deleted successfully.")
                                    }

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Account successfully deleted.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

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
            composable("qrScanner/{targetRoute}") { backStackEntry ->
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val targetRoute = backStackEntry.arguments?.getString("targetRoute") ?: "my_sets"

                val userState by userViewModel.userState.collectAsState()
                val userDatabase = remember { UserDatabase.getDatabase(context) }
                val userFirestore = remember { UserFirestore() }

                qrCameraScreen { scannedValue ->
                    scope.launch {
                        val legoDb = LegoDatabase.getInstance(context)

                        val set = withContext(Dispatchers.IO) {
                            legoDb.getSetByUPC(scannedValue)
                        }

                        Log.d(
                            "LegoTest",
                            "Scanned=$scannedValue name=${set?.name} newPrice=${set?.newPrice} usedPrice=${set?.usedPrice}"
                        )
                        if (set != null && userState.id != 0 && userState.uid.isNotEmpty()) {
                            val roomSet = com.cs407.brickcollector.models.LegoSet(
                                name = set.name,
                                setId = set.setId,
                                price = set.newPrice ?: set.usedPrice ?: 0.0,
                                imageUrl = set.imageUrl
                            )

                            when (targetRoute) {
                                "want_list" -> {
                                    // Add to Want List
                                    withContext(Dispatchers.IO) {
                                        userDatabase.legoDao()
                                            .insertWantListSet(userState.id, roomSet)
                                    }
                                    userFirestore.addSetToWantList(userState.uid, roomSet)
                                }

                            "sell" -> {
                                withContext(Dispatchers.IO) {
                                    userDatabase.legoDao().insertSellListSet(userState.id, roomSet)
                                }
                                userFirestore.addSetToSellList(userState.uid, roomSet)

                            }
                        else -> {
                                // Default: add to My Sets
                                withContext(Dispatchers.IO) {
                                    userDatabase.legoDao().insertMyListSet(userState.id, roomSet)
                                }
                                userFirestore.addSetToMyList(userState.uid, roomSet)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Added to My Sets: ${roomSet.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                    withContext(Dispatchers.Main) {
                        val message = when {
                            set == null -> "No set found for this barcode"
                            userState.id == 0 || userState.uid.isEmpty() ->
                                "You must be logged in to save sets"
                            else -> "Error adding set"
                        }
                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                        }




                            MainActivity.AppState.cameraOn = false
                        navController.popBackStack()
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
                    if (MainActivity.AppState.cameraOn) {
                        MainActivity.AppState.cameraOn = false
                        navController.popBackStack()
                    }

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