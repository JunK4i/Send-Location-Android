package com.example.sendlocationdata

import androidx.lifecycle.MutableLiveData

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

object LocationLiveData {

    val locationData: MutableLiveData<LocationData> = MutableLiveData()
}