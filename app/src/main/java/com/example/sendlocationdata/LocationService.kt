package com.example.sendlocationdata

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.nfc.Tag
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sendlocationdata.AESUtils.encryptData
import com.example.sendlocationdata.AESUtils.generateIV
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.google.gson.Gson


// This runs as a foreground to provide location updates. It will run with persistent notification
class LocationService: Service() {

        // Coroutine scope for the service using IO dispatcher for background work and a SupervisorJob.
        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private lateinit var locationClient: LocationClient

        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        // Initialize the location client.
        override fun onCreate(){
            super.onCreate()
            locationClient = DefaultLocationClient(
                applicationContext,
                LocationServices.getFusedLocationProviderClient(applicationContext)
            )
        }

        // Handles start commands for the service. Starts or stops the service based on the intent action
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            when(intent?.action){
                ACTION_START -> start()
                ACTION_STOP -> stop()
            }
            Log.d(intent?.action,"Service onStartCommand", )
            return super.onStartCommand(intent, flags, startId)
        }

        // Starts location tracking by showing a persistent foreground notification and listening for updates
        private fun start(){
            Log.d("START","Service onStartCommand", )
            val notification = NotificationCompat.Builder(this, "location")
                .setContentTitle("Tracking location...")
                .setContentText("Location: null")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            val notificationManager:NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val gson = Gson()
            // Update notification with new locations, launch the flow in the service's coroutine scope
            locationClient.getLocationUpdates(500L)
                .catch { e -> e.printStackTrace()}
                .onEach { location ->
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                            Date()
                        )
                    )
                    LocationLiveData.locationData.postValue(locationData)
                    val updateNotification = notification.setContentText(
                        "Location: (${locationData.latitude},${locationData.longitude})"
                    )
                    notificationManager.notify(1, updateNotification.build())

                    val jsonData = gson.toJson(locationData)
                    val iv = generateIV()
                    val encryptedData = encryptData(jsonData, "a12621b885b98c469b3c22a93fc1e08874d04a8ee9934aa029a142a16f11e527".hexStringToByteArray(), iv)
                    val encryptedDataWithIV = iv + encryptedData
                    var encryptedDataWithIVBase64 = Base64.encodeToString(encryptedDataWithIV, Base64.DEFAULT)
                    encryptedDataWithIVBase64 = encryptedDataWithIVBase64.replace("\\s".toRegex(), "")
                    Log.d("IV", Base64.encodeToString(iv,Base64.DEFAULT))
                    Log.d("IV", encryptedDataWithIVBase64)
                    sendEncryptedLocationData(encryptedDataWithIVBase64)
                }
                .launchIn(serviceScope)

            // Init the service in the foreground with the initial notification
            startForeground(1, notification.build())

        }

        // Stops notification and service
        private fun stop(){
            LocationLiveData.locationData.postValue(null)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }

        // Cancels the coroutine scope to clean up. As defined in the locationClient, the tracking will stop onDestroy.
        override fun onDestroy() {
            super.onDestroy()
            serviceScope.cancel()
        }

        private fun String.hexStringToByteArray(): ByteArray {
            return this.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        private suspend fun sendEncryptedLocationData(encryptedData: String) {
            if (!isNetworkAvailable(applicationContext)) {
                Log.e("LocationService", "Network is not available, cannot send location data.")
                return
            }

            withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient()
                    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                    val jsonPayload = JSONObject().apply {
                        put("encryptedData", encryptedData)
                    }.toString()
                    val body = jsonPayload.toRequestBody(jsonMediaType)

                    val request = Request.Builder()
                        .url("https://express-server-production-04ab.up.railway.app/api/location/")
//                        .url("http://localhost:3333/api/location")
                        .post(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.e("LocationService", "Failed to send encrypted data: ${response.body?.string()}")
                        } else {
                            Log.d("LocationService", "Encrypted data sent successfully: ${response.body?.string()}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocationService", e.toString())
                }
            }
        }


        // Define constant for strings
        companion object{
            const val ACTION_START = "ACTION_START"
            const val ACTION_STOP = "ACTION_STOP"
            const val ACTION_LOCATION_UPDATE = "ACTION_LOCATION_UPDATE"
            const val LATITUDE = "LATITUDE"
            const val LONGITUDE = "LONGITUDE"
            const val TIMESTAMP = "TIMESTAMP"
        }


}