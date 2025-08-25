package sk.upjs.vmajedalen

class OCRRepository(private val database: AppDatabase) {

    suspend fun saveReceipt(
        items: List<Triple<String, Int, Double>>,
        total: Double,
        date: String,
        time: String
    ) {
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

        //to avoid duplicates of foods
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
    }
}
