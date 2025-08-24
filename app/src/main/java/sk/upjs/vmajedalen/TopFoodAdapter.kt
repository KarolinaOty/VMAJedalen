package sk.upjs.vmajedalen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopFoodAdapter : RecyclerView.Adapter<TopFoodAdapter.ViewHolder>() {

    private var data: List<Pair<String, Int>> = emptyList() // Pair<FoodName, Count>

    fun updateData(newData: List<Pair<String, Int>>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, count) = data[position]
        holder.bind(name, count)
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv1: TextView = itemView.findViewById(android.R.id.text1)
        private val tv2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(name: String, count: Int) {
            tv1.text = name
            tv2.text = "Kúpené $count x"
        }
    }
}
