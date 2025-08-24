package sk.upjs.vmajedalen


class RecordsRepository(private val db: AppDatabase) {

    //food
    suspend fun insertFood(food: Food): Long = db.foodDao().insertFood(food)
    suspend fun getFoodByName(name: String): Food? = db.foodDao().getFoodByName(name)
    suspend fun getAllFoods(): List<Food> = db.foodDao().getAllFoods()

    //lunch
    suspend fun insertLunch(lunch: Lunch): Long = db.lunchDao().insertLunch(lunch)
    suspend fun getAllLunches(): List<Lunch> = db.lunchDao().getAllLunches()
    suspend fun getLunchesByYearMonth(year: String, month: String): List<Lunch> = db.lunchDao().getLunchesByYearMonth(year, month)

    //lunch item
    suspend fun insertLunchItem(item: LunchItem) = db.lunchItemDao().insertLunchItem(item)
    suspend fun getItemsForLunch(lunchId: Int): List<LunchItem> = db.lunchItemDao().getItemsForLunch(lunchId)
    suspend fun getAllLunchItems(): List<LunchItem> = db.lunchItemDao().getAllLunchItems()

    //lunch with items
    suspend fun getLunchesWithItems(): List<LunchWithItems> = db.lunchWithItemsDao().getLunchesWithItems()
    suspend fun getLunchWithItems(id: Int): LunchWithItems = db.lunchWithItemsDao().getLunchWithItems(id)
}
