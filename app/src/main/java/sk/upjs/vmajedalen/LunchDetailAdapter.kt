package sk.upjs.vmajedalen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LunchItemAdapter : RecyclerView.Adapter<LunchItemAdapter.ViewHolder>() {

    private var data: List<LunchItemWithFood> = emptyList()

    fun updateData(newData: List<LunchItemWithFood>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.food_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantityPrice: TextView = itemView.findViewById(R.id.tvItemQuantityPrice)

        fun bind(itemWithFood: LunchItemWithFood) {
            tvName.text = itemWithFood.food.name
            val quantity = itemWithFood.item.quantity
            val price = itemWithFood.item.price
            val finalPrice = quantity * price
            tvQuantityPrice.text = "$quantity * %.2f = %.2f €".format(price, finalPrice)
        }
    }
}

