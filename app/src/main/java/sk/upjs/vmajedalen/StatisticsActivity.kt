package sk.upjs.vmajedalen

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StatisticsActivity : AppCompatActivity() {

    private lateinit var inputYear: EditText
    private lateinit var inputMonth: EditText
    private lateinit var btnFilter: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvAveragePerOrder: TextView
    private lateinit var tvAverageLunchTime: TextView
    private lateinit var tvDaysAttended: TextView
    private lateinit var recyclerViewTopFoods: RecyclerView

    private lateinit var adapter: TopFoodAdapter
    private lateinit var viewModel: StatisticsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        inputYear = findViewById(R.id.inputYear)
        inputMonth = findViewById(R.id.inputMonth)
        btnFilter = findViewById(R.id.btnFilter)
        progressBar = findViewById(R.id.progressBar)

        tvTotalSpent = findViewById(R.id.tvTotalSpent)
        tvAveragePerOrder = findViewById(R.id.tvAveragePerOrder)
        tvAverageLunchTime = findViewById(R.id.tvAverageLunchTime)
        tvDaysAttended = findViewById(R.id.tvDaysAttended)
        recyclerViewTopFoods = findViewById(R.id.recyclerViewTopFoods)

        adapter = TopFoodAdapter()
        recyclerViewTopFoods.layoutManager = LinearLayoutManager(this)
        recyclerViewTopFoods.adapter = adapter

        val database = AppDatabase.getDatabase(this)
        val repository = RecordsRepository(database)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return StatisticsViewModel(repository) as T
                }
            }
        )[StatisticsViewModel::class.java]

        viewModel.totalSpent.observe(this) { tvTotalSpent.text = "%.2f €".format(it) }
        viewModel.averagePerDay.observe(this) { tvAveragePerOrder.text = "%.2f €".format(it) }
        viewModel.averageLunchTime.observe(this) { tvAverageLunchTime.text = "$it" }
        viewModel.daysAttended.observe(this) { tvDaysAttended.text = "$it" }
        viewModel.topFoods.observe(this) { adapter.updateData(it) }
        viewModel.loading.observe(this) { progressBar.visibility = if (it) View.VISIBLE else View.GONE }

        btnFilter.isEnabled = false
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnFilter.isEnabled = inputYear.text.isNotBlank() && inputMonth.text.isNotBlank()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        inputYear.addTextChangedListener(watcher)
        inputMonth.addTextChangedListener(watcher)

        btnFilter.setOnClickListener {
            val year = inputYear.text.toString().trim()
            val month = inputMonth.text.toString().trim().padStart(2,'0')
            if (year.isNotEmpty() && month.isNotEmpty()) {
                viewModel.loadStatistics(year, month)
            }
        }
    }
}
