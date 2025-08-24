package sk.upjs.vmajedalen

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lunch_detail)

        recyclerView = findViewById(R.id.recyclerViewLunchItems)
        tvTotal = findViewById(R.id.tvTotalDetail)

        adapter = LunchItemAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val lunchId = intent.getIntExtra(EXTRA_LUNCH_ID, -1)
        if (lunchId != -1) {
            loadLunchItems(lunchId)
        }
    }

    private fun loadLunchItems(lunchId: Int) {
        val database = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val lunchWithItems = database.lunchWithItemsDao().getLunchWithItems(lunchId)
            val items = lunchWithItems.items

            withContext(Dispatchers.Main) {
                adapter.updateData(items)
                val total = items.sumOf { it.item.price * it.item.quantity }
                tvTotal.text = "Total: %.2f €".format(total)
            }
        }
    }
}
