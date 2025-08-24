package sk.upjs.vmajedalen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class RecordsViewModel(private val repository: RecordsRepository) : ViewModel() {

    private val _lunches = MutableLiveData<List<Lunch>>()
    val lunches: LiveData<List<Lunch>> get() = _lunches

    private val _total = MutableLiveData<Double>()
    val total: LiveData<Double> get() = _total

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    fun loadLunches(year: String?, month: String?) {
        viewModelScope.launch {
            _loading.value = true

            val data = repository.getLunchesByYearMonth(year ?: "", month ?: "")

            _lunches.value = data.sortedByDescending {
                SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                    .parse("${it.date} ${it.time}")
            }
            _total.value = data.sumOf { it.total }
            _loading.value = false
        }
    }

    class RecordsViewModelFactory(private val repository: RecordsRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return RecordsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
