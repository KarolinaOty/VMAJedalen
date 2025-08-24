package sk.upjs.vmajedalen

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Food::class, Lunch::class, LunchItem::class],
    version = 1,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun lunchDao(): LunchDao
    abstract fun lunchItemDao(): LunchItemDao
    abstract fun lunchWithItemsDao(): LunchWithItemsDao

    //singleton
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "VMA_jedalen_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

//data access objects
@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: Food): Long

    @Query("SELECT * FROM food WHERE name = :name LIMIT 1")
    suspend fun getFoodByName(name: String): Food?

    @Query("SELECT * FROM food")
    suspend fun getAllFoods(): List<Food>
}

@Dao
interface LunchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLunch(lunch: Lunch): Long

    @Query("SELECT * FROM lunches")
    suspend fun getAllLunches(): List<Lunch>

    @Query("SELECT * FROM lunches WHERE date LIKE '%-' || :month || '-' || :year")
    suspend fun getLunchesByYearMonth(year: String, month: String): List<Lunch>

}

@Dao
interface LunchItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLunchItem(item: LunchItem)

    @Query("SELECT * FROM lunch_items WHERE lunchId = :lunchId")
    suspend fun getItemsForLunch(lunchId: Int): List<LunchItem>

    @Query("SELECT * FROM lunch_items")
    suspend fun getAllLunchItems(): List<LunchItem>
}

@Dao
interface LunchWithItemsDao {
    @Query("SELECT * FROM lunches")
    suspend fun getLunchesWithItems(): List<LunchWithItems>

    @Query("SELECT * FROM lunches WHERE id = :lunchId")
    suspend fun getLunchWithItems(lunchId: Int): LunchWithItems
}