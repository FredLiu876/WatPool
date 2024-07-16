import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.watpool.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class PlacesRepository(context: Context) {

    private val placesClient: PlacesClient

    init {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(context)
    }

    fun getAutocompletePredictions(query: String): LiveData<List<String>> {
        // Use live data so users can subscribe for updates of predictions
        val liveData = MutableLiveData<List<String>>()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        // Use places api to get predictions based on provided text
        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            liveData.value = predictions
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
            liveData.value = emptyList()
        }

        return liveData
    }
}
