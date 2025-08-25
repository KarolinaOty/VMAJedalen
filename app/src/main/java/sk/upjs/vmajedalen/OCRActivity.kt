package sk.upjs.vmajedalen

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.ViewModelProvider



class OCRActivity : AppCompatActivity() {


    private lateinit var imageView: ImageView
    private var selectedImageUri: Uri? = null
    private lateinit var viewModel: OCRViewModel
    private lateinit var editableText: EditText
    private lateinit var resultsCard: View
    private lateinit var submitButton: Button

    private var parsedReceiptRaw: String? = null  // keep original parsed text

    // register gallery picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        // apply filter for readability
                        val gpuImage = GPUImage(this).apply { setImage(bitmap) }
                        gpuImage.setFilter(GPUImageContrastFilter(1.5f))
                        val filteredBitmap = gpuImage.bitmapWithFilterApplied
                        imageView.setImageBitmap(filteredBitmap)
                        imageView.visibility = View.VISIBLE

                        findViewById<View>(R.id.btnExtractText).isEnabled = true
                    } catch (e: Exception) {
                        Toast.makeText(this, "Chyba pri načítaní obrázku", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        imageView = findViewById(R.id.imageView)
        editableText = findViewById(R.id.editableText)
        resultsCard = findViewById(R.id.resultsCard)
        submitButton = findViewById(R.id.btnSubmit)

        val database = AppDatabase.getDatabase(this)
        val repository = OCRRepository(database)
        viewModel = ViewModelProvider(this, OCRViewModel.Factory(repository))[OCRViewModel::class.java]

        observeViewModel()

        // button for photo
        findViewById<View>(R.id.btnSelectPhoto).setOnClickListener { openGallery() }

        // button for OCR
        findViewById<View>(R.id.btnExtractText).setOnClickListener {
            selectedImageUri?.let { uri ->
                try {
                    val image = InputImage.fromFilePath(this, uri)

                    findViewById<View>(R.id.loaderLayout).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.statusText).apply {
                        text = "Spracovanie prebieha..."
                        visibility = View.VISIBLE
                    }

                    recognizeText(image)
                } catch (e: IOException) {
                    Toast.makeText(this, "Chyba pri čítaní obrázku", Toast.LENGTH_LONG).show()
                }
            } ?: Toast.makeText(this, "Žiaden obrázok nebol vybratý", Toast.LENGTH_LONG).show()
        }

        // button for submit edited result
        submitButton.setOnClickListener {
            val userEditedText = editableText.text.toString().trim()
            if (userEditedText.isEmpty()) {
                Toast.makeText(this, "Nemôžeš uložiť prázdne dáta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            submitButton.isEnabled = false


            // re-parse edited text (ensures items, total, date, time exist)
            val parsed = parseEditedReceipt(userEditedText)
            if (parsed != null) {
                val (items, total, date, time) = parsed
                viewModel.saveReceipt(items, total, date, time)
            } else {
                Toast.makeText(this, "V editovaných dátach chýba jedlo/suma/dátum/čas", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
            }
        }
    }

    private fun observeViewModel() {
        viewModel.saveResult.observe(this) { result ->
            result.onSuccess { count ->
                Toast.makeText(this, "$count položka/y uložená/é", Toast.LENGTH_SHORT).show()

                // reset activity state
                editableText.text.clear()
                imageView.setImageResource(R.drawable.image_placeholder_background)
                imageView.visibility = View.VISIBLE
                resultsCard.visibility = View.GONE
                submitButton.visibility = View.GONE
                submitButton.isEnabled = true
                selectedImageUri = null
                parsedReceiptRaw = null

                // re-enable buttons
                findViewById<View>(R.id.btnSelectPhoto).isEnabled = true
                findViewById<View>(R.id.btnExtractText).isEnabled = false
            }.onFailure { e ->
                Toast.makeText(this, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                submitButton.isEnabled = true
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun recognizeText(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allLines = visionText.textBlocks.flatMap { it.lines }.toMutableList()

                allLines.sortWith { l1, l2 ->
                    val r1 = l1.boundingBox
                    val r2 = l2.boundingBox
                    if (r1 == null || r2 == null) return@sortWith 0
                    val diffY = r1.top - r2.top
                    if (kotlin.math.abs(diffY) > 10) diffY else r1.left - r2.left
                }

                val sb = StringBuilder()
                allLines.forEach { line -> sb.append(line.text).append("\n") }
                val receiptText = sb.toString()

                // parse but don’t save, show in editable field
                parsedReceiptRaw = receiptText
                editableText.setText(receiptText)

                findViewById<View>(R.id.loaderLayout).visibility = View.GONE
                findViewById<TextView>(R.id.statusText).text = "Spracovanie dokončené"
                findViewById<View>(R.id.btnExtractText).isEnabled = false
                resultsCard.visibility = View.VISIBLE
                submitButton.visibility = View.VISIBLE
            }
            .addOnFailureListener {
                Toast.makeText(this, "Text recognition failed", Toast.LENGTH_LONG).show()
                findViewById<View>(R.id.loaderLayout).visibility = View.GONE
                findViewById<TextView>(R.id.statusText).text = "Spracovanie zlyhalo"
            }
    }

    // Parse edited text again to ensure correctness before saving
    private fun parseEditedReceipt(editedText: String): ParsedReceipt? {
        val lines = editedText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val items = mutableListOf<Triple<String, Int, Double>>()
        var total: Double? = null
        var date: String? = null
        var time: String? = null
        var i = 0
        var itemsEnd = false

        while (i < lines.size && !itemsEnd) {
            val line = lines[i]
            val specialChars = setOf('X', 'K', '*')
            val count = line.count { it in specialChars }
            if (count >= 2) {
                itemsEnd = true
                i++
                continue
            }
            if (i + 2 < lines.size) {
                val nameLine = lines[i]
                val quantityPriceLine = lines[i + 1]
                val (q, p) = parseQuantityAndPrice(quantityPriceLine)
                if (q != null && p != null) {
                    items.add(Triple(nameLine, q, p))
                    i += 3
                } else i++
            } else i++
        }

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.contains("Celkom", ignoreCase = true) && i + 1 < lines.size -> {
                    total = parseAmount(lines[i + 1]); i++
                }
                line.matches(Regex("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}")) -> {
                    val parts = line.split(" ")
                    date = parts[0]; time = parts[1]
                }
            }
            i++
        }

        return if (items.isNotEmpty() && total != null && date != null && time != null) {
            ParsedReceipt(items, total, date, time)
        } else null
    }

    private fun parseQuantityAndPrice(line: String): Pair<Int?, Double?> {
        // Patterns to match: "2 * 1.85", "1x3.50", "3 * 2,00", etc.
        val patterns = listOf(
            Regex("(\\d+)\\s*[*xX]\\s*(\\d+[.,]\\d+)"),  // 2 * 1.85 or 1x3.50
            Regex("(\\d+)\\s*[*xX]\\s*(\\d+)"),          // 2 * 3 (no decimal)
            Regex("(\\d+)\\s+(\\d+[.,]\\d+)"),           // 2 1.85 (space separated)
            Regex("(\\d+)(\\d+[.,]\\d+)")                // 21.85 (no separator)
        )

        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null && match.groupValues.size >= 3) {
                val quantityStr = match.groupValues[1]
                var priceStr = match.groupValues[2]

                // Clean the price string
                priceStr = priceStr.replace(",", ".")
                    .replace("\\s".toRegex(), "")
                    .trim()

                val quantity = quantityStr.toIntOrNull()
                val unitPrice = priceStr.toDoubleOrNull()

                if (quantity != null && unitPrice != null) {
                    return Pair(quantity, unitPrice)
                }
            }
        }

        // Fallback: try to extract just the price if quantity parsing fails
        val price = tryExtractPrice(line)
        return Pair(1, price) // Default quantity to 1 if only price is found
    }

    private fun tryExtractPrice(line: String): Double? {
        // Handle various price formats including OCR errors
        val patterns = listOf(
            Regex("\\d+[.,]\\s*\\d+"),              // 3,70 or 3.70 (unit price)
            Regex("[=*]\\s*\\d+[.,]\\s*\\d+"),      // =3,70 or *3,70 (final price)
            Regex("\\d+\\s*[*xX]\\s*\\d+[.,]\\d+"), // 1 *3,70 (quantity * price)
            Regex("[=*]\\s*\\d+"),                  // =3 or *3 (simple price)
            Regex("\\d+")                           // Just numbers
        )

        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                var priceStr = match.value
                // Clean the price string - handle OCR errors like X0_20 -> 0.20
                priceStr = priceStr.replace("[=*xX_]".toRegex(), "") // Remove =, *, x, X, _
                    .replace(",", ".")
                    .replace("\\s".toRegex(), "") // Remove spaces
                    .trim()

                val price = priceStr.toDoubleOrNull()
                if (price != null) {
                    return price
                }
            }
        }
        return null
    }

    private fun parseAmount(amountLine: String): Double? {
        // Handle amount parsing
        val cleanAmount = amountLine.replace(",", ".")
            .replace("\\s".toRegex(), "") // Remove spaces
            .replace("[^0-9.]".toRegex(), "") // Remove non-numeric except dot
            .trim()

        return cleanAmount.toDoubleOrNull()
    }

    private data class ParsedReceipt(
        val items: List<Triple<String, Int, Double>>,
        val total: Double,
        val date: String,
        val time: String
    )
}
