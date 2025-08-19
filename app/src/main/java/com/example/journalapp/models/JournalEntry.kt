package com.example.journalapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data class JournalEntry(
    val id: Long = 0,
    val content: String,
    val mood: Int = 0, // Range from -10 to +10
    val tags: String = "",
    val date: Long = System.currentTimeMillis()
) : Parcelable {

    fun getFormattedDate(): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(date))
    }
    // Apple-style emojis
    fun getMoodEmoji(): String {
        return when {
            mood <= -7 -> "\uD83D\uDE21" // 😡
            mood <= -3 -> "\uD83D\uDE22" // 😢
            mood <= 3 -> "\uD83D\uDE10"  // 😐
            mood <= 7 -> "\uD83D\uDE42"  // 🙂
            else -> "\uD83D\uDE00"       // 😀
        }
    }
    fun getMoodLabel(): String {
        return when {
            mood <= -7 -> "Furious"
            mood <= -3 -> "Sad"
            mood <= 3 -> "Neutral"
            mood <= 7 -> "Happy"
            else -> "Ecstatic"
        }
    }
}