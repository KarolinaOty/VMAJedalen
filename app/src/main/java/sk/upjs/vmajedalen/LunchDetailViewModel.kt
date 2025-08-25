package sk.upjs.vmajedalen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LunchDetailViewModel(private val repository: LunchRepository) : ViewModel() {
    private val _lunchItems = MutableLiveData<List<LunchItemWithFood>>()
    val lunchItems: LiveData<List<LunchItemWithFood>> = _lunchItems

    private val _total = MutableLiveData<Double>()
    val total: LiveData<Double> = _total

    fun loadLunch(lunchId: Int) {
        viewModelScope.launch {
            val lunchWithItems = repository.getLunchWithItems(lunchId)
            _lunchItems.value = lunchWithItems.items
            _total.value = lunchWithItems.items.sumOf { it.item.price * it.item.quantity }
        }
    }
}

class LunchDetailViewModelFactory(
    private val repository: LunchRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LunchDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LunchDetailViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
