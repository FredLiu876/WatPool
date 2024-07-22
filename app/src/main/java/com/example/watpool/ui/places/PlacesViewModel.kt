import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val placesRepository: PlacesRepository = PlacesRepository(application)

    // So selected value can be sent to required views
    private val selectedPrediction = MutableLiveData<String>()

    fun getAutocompletePredictions(query: String): LiveData<List<String>> {
        return placesRepository.getAutocompletePredictions(query)
    }

    fun setSelectedPrediction(prediction: String) {
        selectedPrediction.value = prediction
    }

    fun getSelectedPrediction(): LiveData<String> {
        return selectedPrediction
    }

    fun clearSelectedPrediction() {
        selectedPrediction.value = ""
    }
}
