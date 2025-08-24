package sk.upjs.vmajedalen

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatisticsViewModel(private val repository: RecordsRepository) : ViewModel() {

    val totalSpent = MutableLiveData<Double>()
    val averagePerDay = MutableLiveData<Double>() // Changed from averagePerOrder
    val averageLunchTime = MutableLiveData<String>()
    val daysAttended = MutableLiveData<Int>()
    val topFoods = MutableLiveData<List<Pair<String, Int>>>()
    val loading = MutableLiveData<Boolean>()
    private val _dailySpending = MutableLiveData<List<Pair<Int, Double>>>()
    val dailySpending: LiveData<List<Pair<Int, Double>>> get() = _dailySpending

    fun loadStatistics(year: String, month: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loading.postValue(true)

            val lunches = repository.getLunchesByYearMonth(year, month)

            // total price
            val total = lunches.sumOf { it.total }
            totalSpent.postValue(total)

            // unique days of visits
            val uniqueDays = lunches.map { it.date }.distinct()
            val daysCount = uniqueDays.count()
            daysAttended.postValue(daysCount)

            // avg per day (total spent / number of unique days)
            val avgPerDay = if (daysCount > 0) total / daysCount else 0.0
            averagePerDay.postValue(avgPerDay)

            // avg time
            val times = lunches.mapNotNull { lunch ->
                try {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).parse(lunch.time)
                } catch (e: Exception) { null }
            }
            val avgTime = if (times.isNotEmpty()) {
                val avgMillis = times.map { it.time }.average().toLong()
                val cal = Calendar.getInstance().apply { timeInMillis = avgMillis }
                String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else "00:00"
            averageLunchTime.postValue(avgTime)

            // top 5 foods
            val allItems = lunches.flatMap { lunch ->
                repository.getItemsForLunch(lunch.id)
            }
            val foodCounts = mutableMapOf<String, Int>()
            for (item in allItems) {
                val food = repository.getFoodById(item.foodId)
                if (food != null) {
                    foodCounts[food.name] = (foodCounts[food.name] ?: 0) + item.quantity
                }
            }
            val top5 = foodCounts.entries.sortedByDescending { it.value }.take(5)
                .map { it.key to it.value }
            topFoods.postValue(top5)

            // Example: calculate money spent per day
            val dailyMap = lunches.groupBy { it.date.split("-")[0].toInt() } // assuming date = dd-MM-yyyy
                .mapValues { entry -> entry.value.sumOf { it.total } }
            val dailyList = (1..31).map { day -> day to (dailyMap[day] ?: 0.0) }

            _dailySpending.postValue(dailyList) // <- changed from .value to .postValue

            loading.postValue(false)
        }
    }
}
