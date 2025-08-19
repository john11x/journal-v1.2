package com.example.journalapp.utils

import com.example.journalapp.db.JournalDatabaseHelper
import java.util.*

class StreakManager(private val dbHelper: JournalDatabaseHelper) {

    fun calculateStreak(): Int {
        val entries = dbHelper.getAllEntries()
        if (entries.isEmpty()) return 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val entryDates = entries.map { entry ->
            Calendar.getInstance().apply { timeInMillis = entry.date }.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct().sortedDescending()

        var streak = 0
        var currentDate = calendar.timeInMillis

        for (date in entryDates) {
            if (date == currentDate) {
                streak++
                currentDate -= 86400000 // Subtract one day
            } else if (date < currentDate) {
                break
            }
        }

        return streak
    }

    fun getCurrentStreakStartDate(): Long? {
        val entries = dbHelper.getAllEntries()
        if (entries.isEmpty()) return null

        val entryDates = entries.map { it.date }.distinct().sortedDescending()
        var currentStreakStart = entryDates.first()
        var previousDate = currentStreakStart

        for (date in entryDates.drop(1)) {
            if (previousDate - date <= 86400000 * 2) { // Allow 1 day gap
                previousDate = date
            } else {
                break
            }
            currentStreakStart = date
        }

        return currentStreakStart
    }
}