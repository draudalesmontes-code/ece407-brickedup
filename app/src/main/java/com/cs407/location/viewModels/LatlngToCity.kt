package com.cs407.location.viewModels

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class LatlngToCity : ViewModel(){
    private val _cityCounty = MutableStateFlow<String?>(null)
    val cityCounty: StateFlow<String?> = _cityCounty.asStateFlow()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.geoapify.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: GeoapifyAPI = retrofit.create(GeoapifyAPI::class.java)
    suspend fun resolveAndStore(latLng: LatLng, apiKey: String, lang: String = "en") {
        try {
            val resp = api.getCityCounty(
                lat = latLng.latitude,
                lon = latLng.longitude,
                apiKey = apiKey,
                limit = 1,
                lang = lang
            )
            val props = resp.features.firstOrNull()?.properties

            // Prefer city; fallback to county
            val result: String? = props?.city ?: props?.county
            _cityCounty.value = result
        } catch (_: Exception) {
            _cityCounty.value = null
        }
    }

    data class GeoapifyResponse(val features: List<Feature>) {
        data class Feature(val properties: Properties)
        data class Properties(
            val city: String? = null,
            val county: String? = null
        )
    }
    interface GeoapifyAPI{
        @GET("v1/geocode/reverse")
        suspend fun getCityCounty(
            @Query("lat") lat: Double,
            @Query("lon") lon: Double,
            @Query("apiKey") apiKey: String,
            @Query("limit") limit: Int = 1,
            @Query("lang") lang: String? = null,
            @Query("format") format: String = "geojson"
        ): GeoapifyResponse
    }






}
