package com.example.watpool.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.watpool.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


class DirectionsRepository {
    suspend fun getDirections(origin: LatLng, dest: LatLng): MutableLiveData<String> {
        val data = MutableLiveData<String>()
        // origin of route
        val originStr = "origin=${origin.latitude},${origin.longitude}"
        // destination of route
        val destinationStr = "destination=${dest.latitude},${dest.longitude}"
        // api key need to add api key
        val key = "key=${BuildConfig.MAPS_API_KEY}"
        // Sensor enabled
        val sensor = "sensor=false"
        val mode = "mode=driving"
        val parameters = "${originStr}&${destinationStr}&${sensor}&${mode}&${key}"
        // output format
        val output = "json"
        val urlStr = "https://maps.googleapis.com/maps/api/directions/${output}?${parameters}"

        withContext(Dispatchers.IO) {
            val result = downloadUrl(urlStr)
            data.postValue(result)
        }
        return data
    }

    private fun downloadUrl(strUrl: String): String {
        var data = ""
        var iStream: InputStream? = null
        var urlConnection: HttpURLConnection? = null
        try {
            val url = URL(strUrl)
            urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.connect()

            iStream = urlConnection.inputStream

            val br = BufferedReader(InputStreamReader(iStream))
            val sb = StringBuffer()

            var line: String? = br.readLine()
            while (line != null) {
                sb.append(line)
                line = br.readLine()
            }
            data = sb.toString()
            br.close()
        } catch (e: Exception) {
            Log.e("Directions exception", e.toString())
        } finally {
            iStream?.close()
            urlConnection?.disconnect()
        }
        return data
    }


}