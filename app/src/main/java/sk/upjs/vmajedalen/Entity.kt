package sk.upjs.vmajedalen

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

//jedna polozka jedla z blocka
@Entity(tableName = "food")
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

//jeden obed = jeden blocek
@Entity(tableName = "lunches")
data class Lunch(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val total: Double
)

// m-n vztah medzi polozkami jedal a obedmi
@Entity(
    tableName = "lunch_items",
    foreignKeys = [
        ForeignKey(
            entity = Lunch::class,
            parentColumns = ["id"],
            childColumns = ["lunchId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lunchId"), Index("foodId")]
)

data class LunchItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lunchId: Int,
    val foodId: Int,
    val price: Double
)

data class LunchWithItems(
    @Embedded val lunch: Lunch,
    @Relation(
        parentColumn = "id",
        entityColumn = "lunchId",
        entity = LunchItem::class
    )
    val items: List<LunchItemWithFood>
)

data class LunchItemWithFood(
    @Embedded val item: LunchItem,
    @Relation(
        parentColumn = "foodId",
        entityColumn = "id"
    )
    val food: Food
)
