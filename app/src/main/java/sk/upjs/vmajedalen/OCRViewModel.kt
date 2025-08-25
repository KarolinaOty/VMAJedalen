package sk.upjs.vmajedalen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope

class OCRViewModel(private val repository: OCRRepository) : ViewModel() {

    private val _saveResult = MutableLiveData<Result<Int>>()
    val saveResult: LiveData<Result<Int>> = _saveResult

    fun saveReceipt(
        items: List<Triple<String, Int, Double>>,
        total: Double,
        date: String,
        time: String
    ) {
        viewModelScope.launch {
            try {
                repository.saveReceipt(items, total, date, time)
                _saveResult.postValue(Result.success(items.size))
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }

    class Factory(private val repository: OCRRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OCRViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OCRViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
