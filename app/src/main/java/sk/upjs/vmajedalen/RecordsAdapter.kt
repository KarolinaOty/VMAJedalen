package sk.upjs.vmajedalen

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecordsAdapter : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    private var data: List<Lunch> = emptyList()

    fun updateData(newData: List<Lunch>) {
        data = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            RecordViewHolder { val view = LayoutInflater.from(parent.context)
        .inflate(R.layout.lunch, parent, false)
        return RecordViewHolder(view) }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(lunch: Lunch) {
            tvDate.text = lunch.date
            tvTime.text = lunch.time
            tvPrice.text = "%.2f €".format(lunch.total)

            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, LunchDetailActivity::class.java)
                intent.putExtra(LunchDetailActivity.EXTRA_LUNCH_ID, lunch.id)
                context.startActivity(intent)
            }
        }
    }
}
