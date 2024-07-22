import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.SearchView
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.watpool.R

class PlacesFragment : Fragment() {

    private lateinit var placesViewModel: PlacesViewModel

    private lateinit var listView: ListView

    private lateinit var listViewAdapter: ArrayAdapter<String>
    private lateinit var predictionsList: MutableList<String>
    private var maxItems: Int = 4



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_places, container, false)

        listView = view.findViewById(R.id.placesList)

        predictionsList = mutableListOf()
        listViewAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, predictionsList)
        listView.adapter = listViewAdapter

        placesViewModel = ViewModelProvider(requireActivity()).get(PlacesViewModel::class.java)

        // When item in list is selected add it to live data for views to use
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedItem = listViewAdapter.getItem(position)
            if (selectedItem != null) {
                placesViewModel.setSelectedPrediction(selectedItem)
                clearList()
            }
        }

        return view
    }

    fun updateList(items: List<String>) {
        predictionsList.clear()
        predictionsList.addAll(items.take(maxItems)) // Limit the number of items added
        listViewAdapter.notifyDataSetChanged()
    }

    fun clearList(){
        predictionsList.clear()
        listViewAdapter.notifyDataSetChanged()
    }

    fun setMaxItems(maxItems: Int) {
        this.maxItems = maxItems
    }
}
