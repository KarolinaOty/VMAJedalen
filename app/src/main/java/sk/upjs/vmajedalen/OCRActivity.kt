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



class OCRActivity : AppCompatActivity() {

    private lateinit var tv: TextView
    private lateinit var imageView: ImageView

    private var selectedImageUri: Uri? = null


    // Register gallery picker
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                selectedImageUri?.let { uri ->
                    try {
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        // Apply GPU filter
                        val gpuImage = GPUImage(this).apply { setImage(bitmap) }
                        gpuImage.setFilter(GPUImageContrastFilter(1.5f))
                        val filteredBitmap = gpuImage.bitmapWithFilterApplied
                        imageView.setImageBitmap(filteredBitmap)
                        imageView.visibility = View.VISIBLE
                        findViewById<View>(R.id.btnExtractText).isEnabled = true
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error loading picture", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        tv = findViewById(R.id.textView)
        imageView = findViewById(R.id.imageView)

        // button for photo
        findViewById<View>(R.id.btnSelectPhoto).setOnClickListener {
            openGallery()
        }

        // button for OCR
        findViewById<View>(R.id.btnExtractText).setOnClickListener {
            selectedImageUri?.let { uri ->
                try {
                    val image = InputImage.fromFilePath(this, uri)

                    // Show loader + status
                    findViewById<View>(R.id.loaderLayout).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.statusText).apply {
                        text = "Spracovanie prebieha..."
                        visibility = View.VISIBLE
                    }

                    recognizeText(image)
                } catch (e: IOException) {
                    Toast.makeText(this, "Error reading image", Toast.LENGTH_LONG).show()
                }
            } ?: Toast.makeText(this, "No image selected", Toast.LENGTH_LONG).show()
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
                tv.text = receiptText

                //hide, disable button, show results
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                findViewById<TextView>(R.id.statusText).text = "Spracovanie dokončené"
                findViewById<View>(R.id.btnExtractText).isEnabled = false
                findViewById<View>(R.id.resultsCard).visibility = View.VISIBLE
                parseAndSaveReceipt(receiptText)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Text recognition failed", Toast.LENGTH_LONG).show()

                // Hide, update
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                findViewById<TextView>(R.id.statusText).text = "Spracovanie zlyhalo"
            }
    }

    private fun parseAndSaveReceipt(receiptText: String) {
        val lines = receiptText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val items = mutableListOf<Triple<String, Int, Double>>() // Changed to Triple: (name, quantity, unitPrice)
        var total: Double? = null
        var date: String? = null
        var time: String? = null

        var i = 0
        var itemsEnd = false

        // Process items (groups of 3 lines) until we find the X,K,* line
        while (i < lines.size && !itemsEnd) {
            val line = lines[i]

            // Check for end marker (line with XK* pattern)
            if (line.contains("XK*") || hasRepeatedChars(line, 2, setOf('X', '*', 'K'))) {
                itemsEnd = true
                i++
                continue
            }

            // Process item group (3 lines): NAME -> QUANTITY * PRICE -> = FINAL PRICE
            if (i + 2 < lines.size) {
                val nameLine = lines[i]
                val quantityPriceLine = lines[i + 1]
                val finalPriceLine = lines[i + 2]

                // Parse quantity and unit price from the second line
                val (quantity, unitPrice) = parseQuantityAndPrice(quantityPriceLine)

                if (quantity != null && unitPrice != null && nameLine.isNotBlank()) {
                    items.add(Triple(nameLine, quantity, unitPrice))
                    i += 3 // Skip the processed lines
                } else {
                    i++ // Move to next line if not a valid item
                }
            } else {
                i++ // Not enough lines for a complete item
            }
        }

        // Look for total and date/time after items section
        while (i < lines.size) {
            val line = lines[i]

            when {
                line.contains("Celkom", ignoreCase = true) -> {
                    if (i + 1 < lines.size) {
                        total = parseAmount(lines[i + 1])
                        i++ // Skip total label line
                    }
                }

                line.matches(Regex("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}")) -> {
                    val parts = line.split(" ")
                    date = parts[0]
                    time = parts[1]
                }
            }
            i++
        }

        if (items.isNotEmpty() && total != null && date != null && time != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {

                    val database = AppDatabase.getDatabase(this@OCRActivity)
                    val foodDao = database.foodDao()
                    val lunchDao = database.lunchDao()
                    val lunchItemDao = database.lunchItemDao()


                    val lunchId = lunchDao.insertLunch(
                        Lunch(
                            date = date,
                            time = time,
                            total = total
                        )
                    ).toInt()


                    items.forEach { (name, quantity, unitPrice) ->
                        var food = foodDao.getFoodByName(name)
                        if (food == null) {
                            val foodId = foodDao.insertFood(Food(name = name))
                            food = Food(foodId.toInt(), name)
                        }

                        lunchItemDao.insertLunchItem(
                            LunchItem(
                                lunchId = lunchId,
                                foodId = food.id,
                                quantity = quantity,
                                price = unitPrice
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OCRActivity, "Saved ${items.size} items successfully", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OCRActivity, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            val errorMsg = buildString {
                append("Missing: ")
                if (items.isEmpty()) append("items ")
                if (total == null) append("total ")
                if (date == null) append("date ")
                if (time == null) append("time ")
            }
            Toast.makeText(this, "Parse failed: $errorMsg", Toast.LENGTH_LONG).show()
        }
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

    // Update your tryExtractPrice function to handle unit prices better
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

    private fun hasRepeatedChars(line: String, minCount: Int, charsToCheck: Set<Char>): Boolean {
        if (line.length < minCount) return false

        for (char in charsToCheck) {
            // Check if line contains multiple occurrences of the character
            val charCount = line.count { it.equals(char, ignoreCase = true) }
            if (charCount > minCount) {
                return true
            }
        }
        return false
    }
}
