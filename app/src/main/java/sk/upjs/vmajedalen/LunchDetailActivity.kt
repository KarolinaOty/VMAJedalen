package sk.upjs.vmajedalen

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LunchDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LUNCH_ID = "extra_lunch_id"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LunchItemAdapter
    private lateinit var tvTotal: TextView
    private lateinit var viewModel: LunchDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lunch_detail)

        recyclerView = findViewById(R.id.recyclerViewLunchItems)
        tvTotal = findViewById(R.id.tvTotalDetail)

        adapter = LunchItemAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val database = AppDatabase.getDatabase(this)
        val repository = LunchRepository(database)
        viewModel = ViewModelProvider(
            this,
            LunchDetailViewModelFactory(repository)
        )[LunchDetailViewModel::class.java]

        viewModel.lunchItems.observe(this) { adapter.updateData(it) }
        viewModel.total.observe(this) { tvTotal.text = "Total: %.2f €".format(it) }

        val lunchId = intent.getIntExtra(EXTRA_LUNCH_ID, -1)
        if (lunchId != -1) {
            viewModel.loadLunch(lunchId)
        }
    }
}
