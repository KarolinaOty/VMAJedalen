package sk.upjs.vmajedalen

class LunchRepository(private val db: AppDatabase) {
    suspend fun getLunchWithItems(lunchId: Int): LunchWithItems {
        return db.lunchWithItemsDao().getLunchWithItems(lunchId)
    }
}
