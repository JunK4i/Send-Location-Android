package com.example.sendlocationdata

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.sendlocationdata.ui.theme.SendLocationDataTheme

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_NETWORK_STATE
            ),
            0
        )

        setContent {
            SendLocationDataTheme {
                val context = LocalContext.current
                val locationData = LocationLiveData.locationData.observeAsState().value
                var aesKey by remember { mutableStateOf<String?>(null) }

                // Launch service immediately
                LaunchedEffect(Unit){
                    Intent(context, LocationService::class.java).also { intent ->
                        intent.action = LocationService.ACTION_START
                        ContextCompat.startForegroundService(context, intent)
                    }
                }
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    locationData?.let { data ->
                        Text(
                            text = "Latitude: ${data.latitude}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Longitude: ${data.longitude}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = "Timestamp: ${data.timestamp}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } ?: Text(text = "Waiting for location update...", style = MaterialTheme.typography.bodyLarge)

                    Button(onClick = {
                        Intent(applicationContext, LocationService::class.java).also {intent ->
                            intent.action = LocationService.ACTION_START
                            ContextCompat.startForegroundService(context,intent)
                        }
                    }, modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                        .height(60.dp)

                    ) {
                        Text(text = "Start", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                            startService(this) // Send command to the service
                        }
                    }, modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.8f)
                        .height(60.dp)
                    ) {
                        Text(text = "Stop", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

}

// Use AlarmManager to wakeup the service when open app
// use broadcast receiver to deliver app-wide services, can create a status bar notification. Gateway for other components such as a job service
// Intent provides information for launching activities. passive data structure