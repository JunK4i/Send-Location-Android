package com.example.sendlocationdata

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

 class DefaultLocationClient (
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient{

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        // Create a callbackFlow to emit location updates
        return callbackFlow {

            // Check for location permission
             if(!context.hasLocationPermission()){
                 throw LocationClient.LocationException("Missing location permission")
             }

            // Check for gps enabled
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if(!isGpsEnabled && !isNetworkEnabled){
                throw LocationClient.LocationException("GPS or Network is disabled")
            }

            // Configure the location request
            val request = com.google.android.gms.location.LocationRequest.Builder(interval)
                .setMinUpdateIntervalMillis(interval)
                .build()

            // Define the locationCallback
            val locationCallback = object : LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    // Take the last known location and emit it through the callbackFlow
                    result.locations.lastOrNull()?.let { location ->
                        // Launch a new coroutine for sending location updates whilst preventing blocking
                        launch { send(location) }
                    }
                }
            }

            // Request location updates with the configured request and callback
            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper() // Callbacks will be received on the main thread
            )

            // When the flow collector is cancelled or the flow completes, remove location updates
            awaitClose{
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}