package com.cs407.location.viewModels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await


class LocationViewModel : ViewModel(){

    private val _currentLatLng = MutableStateFlow<LatLng?>(null)
    val currentLatLng: StateFlow<LatLng?> = _currentLatLng.asStateFlow()



    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getCurrentLocation(): LatLng? {
        if (!hasPermission) return null
        val client = fusedLocationClient
        return try {
            val loc = client.lastLocation.await()          // coroutine, no callbacks
            val latLng = loc?.let { LatLng(it.latitude, it.longitude) }
            _currentLatLng.value = latLng                         // publish for observers
            latLng
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {                           // services off / null / timeout (if you add one)
            null
        }
    }
    private var hasPermission = false

    fun updateLocationPermission(granted: Boolean) {
        hasPermission = granted
    }

}