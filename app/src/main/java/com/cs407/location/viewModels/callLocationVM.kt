// callLocationVM.kt  (only the changed/added pieces)
package com.cs407.location.viewModels

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class callLocationVM(
    private val locationVM: LocationViewModel = LocationViewModel(),
    private val cityVM: LatlngToCity = LatlngToCity()
) : ViewModel() {

    private var apiKey: String? = null
    private var hasPermission: Boolean = false

    private val _city = MutableStateFlow<String?>(null)
    val city: StateFlow<String?> = _city.asStateFlow()

    fun initialize(context: Context, geoapifyApiKey: String) {
        apiKey = geoapifyApiKey
        locationVM.initializeLocationClient(context)
    }

    fun updatePermission(granted: Boolean) {
        hasPermission = granted
        locationVM.updateLocationPermission(granted)
    }

    // Public checker the Activity can call
    fun hasLocationPermission(context: Context): Boolean =
        android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                android.content.pm.PackageManager.PERMISSION_GRANTED ==
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                )

    /** Call this ONLY after permission is granted. */
    suspend fun resolveCityAssumingPermission(
        appContext: Context,
        geoapifyApiKey: String,
        lang: String = "en"
    ): String {
        if (apiKey == null) {
            apiKey = geoapifyApiKey
            locationVM.initializeLocationClient(appContext)
        }
        if (!hasPermission) return storeAndReturn("none")
        val key = apiKey ?: return storeAndReturn("none")

        val latLng: LatLng = locationVM.getCurrentLocation() ?: return storeAndReturn("none")
        return try {
            cityVM.resolveAndStore(latLng, apiKey = key, lang = lang)
            storeAndReturn(cityVM.cityCounty.value ?: "none")
        } catch (_: Exception) {
            storeAndReturn("none")
        }
    }
    suspend fun fetchLatLngOnce(): com.google.android.gms.maps.model.LatLng? {
        return locationVM.getCurrentLocation()
    }

    private fun storeAndReturn(value: String): String {
        _city.value = value
        return value
    }
}
