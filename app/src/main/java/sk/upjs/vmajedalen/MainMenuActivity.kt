package sk.upjs.vmajedalen

import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import kotlinx.coroutines.Dispatchers
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(this@MainMenuActivity)
        }

        //buttons
        val btnOcr = findViewById<Button>(R.id.btnOcr)
        val btnRecords = findViewById<Button>(R.id.btnRecords)
        val btnStats = findViewById<Button>(R.id.btnStats)

        //listeners
        btnOcr.setOnClickListener {
            startActivity(Intent(this, OCRActivity::class.java))
        }
        btnRecords.setOnClickListener {
            startActivity(Intent(this, RecordsActivity::class.java))
        }
        btnStats.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }
}