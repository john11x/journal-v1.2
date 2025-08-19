package com.example.journalapp.views

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.journalapp.R
import com.example.journalapp.databinding.ItemJournalBinding
import com.example.journalapp.models.JournalEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class JournalAdapter(
    private val onClick: (JournalEntry) -> Unit,
    private val onDelete: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DiffCallback()) {

    inner class JournalViewHolder(private val binding: ItemJournalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntry) {
            with(binding) {
                // Date formatting
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                dateText.text = dateFormat.format(Date(entry.date))

                // Content preview
                previewText.text = entry.content.take(120).let {
                    if (entry.content.length > 120) "$it..." else it
                }

                // Tags handling
                tagContainer.removeAllViews()
                entry.tags.split(",")
                    .filter { it.isNotBlank() }
                    .forEach { tag ->
                        val chip = com.google.android.material.chip.Chip(root.context).apply {
                            text = tag.trim()
                            isCheckable = false
                            setChipBackgroundColorResource(R.color.tag_background)
                            setTextColor(ContextCompat.getColor(context, R.color.tag_text))
                            chipStrokeWidth = 0.5f
                            chipStrokeColor = ColorStateList.valueOf(
                                ContextCompat.getColor(context, R.color.tag_border)
                            )
                            setEnsureMinTouchTargetSize(false)
                        }
                        tagContainer.addView(chip)
                    }

                // Mood emoji with color coding
                val (emoji, color) = when {
                    entry.mood <= -7 -> Pair("😠", R.color.mood_angry)
                    entry.mood <= -3 -> Pair("😔", R.color.mood_sad)
                    entry.mood <= 3 -> Pair("😐", R.color.mood_neutral)
                    entry.mood <= 7 -> Pair("😊", R.color.mood_happy)
                    else -> Pair("😄", R.color.mood_ecstatic)
                }
                moodEmoji.text = emoji
                moodEmoji.setTextColor(ContextCompat.getColor(root.context, color))

                // Click listener
                root.setOnClickListener { onClick(entry) }

                // Delete button
                deleteButton.setOnClickListener {
                    onDelete(entry)
                }

                // Animation
                root.alpha = 0f
                root.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        return JournalViewHolder(
            ItemJournalBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}