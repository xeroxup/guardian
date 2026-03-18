package com.guardian.app.ui.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.guardian.app.R
import com.guardian.app.data.LogEntry
import com.guardian.app.data.LogType
import com.guardian.app.databinding.ItemLogBinding

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(entry: LogEntry) {
            binding.logTitle.text = entry.title
            binding.logDesc.text = entry.desc
            binding.logTime.text = entry.time
            
            val (iconRes, bgRes, tintColor) = when (entry.type) {
                LogType.BLOCK -> Triple(R.drawable.ic_shield, R.drawable.circle_background_green, R.color.green_success)
                LogType.THREAT -> Triple(R.drawable.ic_alert, R.drawable.circle_background_red, R.color.red_danger)
                LogType.CHECK -> Triple(R.drawable.ic_check, R.drawable.circle_background_blue, R.color.primary)
            }
            
            binding.logIcon.setImageResource(iconRes)
            binding.logIconBackground.setBackgroundResource(bgRes)
            binding.logIcon.setColorFilter(binding.root.context.getColor(tintColor))
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
