package com.example.dice.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dice.R
import com.example.dice.model.RollResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultAdapter(private var items: List<RollResult>, private val onLongClick: (RollResult) -> Unit) : RecyclerView.Adapter<ResultAdapter.VH>() {
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDice: TextView = itemView.findViewById(R.id.tvDice)
        val tvRolls: TextView = itemView.findViewById(R.id.tvRolls)
        val tvSum: TextView = itemView.findViewById(R.id.tvSum)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_roll_result, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvDice.text = "${item.dice.count}d${item.dice.sides}"
        holder.tvRolls.text = "结果: ${item.rolls.joinToString(", ")}"
        holder.tvSum.text = "总和: ${item.sum}"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        holder.tvTime.text = df.format(Date(item.timestamp))
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    fun submitList(newItems: List<RollResult>) {
        items = newItems
        notifyDataSetChanged()
    }
}
