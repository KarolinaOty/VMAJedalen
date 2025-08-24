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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecordsActivity : AppCompatActivity() {

    private lateinit var inputYear: EditText
    private lateinit var inputMonth: EditText
    private lateinit var btnFilter: Button
    private lateinit var tvTotal: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: RecordsAdapter
    private lateinit var viewModel: RecordsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)

        initializeViews()
        setupRecyclerView()
        setupViewModel()
        observeViewModel()
    }

    private fun initializeViews() {
        inputYear = findViewById(R.id.inputYear)
        inputMonth = findViewById(R.id.inputMonth)
        btnFilter = findViewById(R.id.btnFilter)
        tvTotal = findViewById(R.id.tvTotal)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewRecords)

        btnFilter.isEnabled = false

        //need to input both to unblock button
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnFilter.isEnabled = inputYear.text.isNotEmpty() && inputMonth.text.isNotEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        inputYear.addTextChangedListener(watcher)
        inputMonth.addTextChangedListener(watcher)

        btnFilter.setOnClickListener {
            val year = inputYear.text.toString().trim()
            val month = inputMonth.text.toString().trim().padStart(2, '0')
            viewModel.loadLunches(year, month)
        }
    }

    private fun setupRecyclerView() {
        adapter = RecordsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(this)
        val repository = RecordsRepository(database)

        viewModel = ViewModelProvider(
            this,
            RecordsViewModel.RecordsViewModelFactory(repository)
        )[RecordsViewModel::class.java]
    }

    private fun observeViewModel() {
        viewModel.lunches.observe(this) { lunches ->
            adapter.updateData(lunches)
        }

        viewModel.total.observe(this) { total ->
            tvTotal.text = "Total: %.2f €".format(total)
        }

        viewModel.loading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }
}
