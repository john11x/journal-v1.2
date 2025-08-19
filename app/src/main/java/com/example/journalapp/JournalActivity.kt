package com.example.journalapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.journalapp.databinding.ActivityJournalBinding
import com.example.journalapp.db.JournalDatabaseHelper
import com.example.journalapp.models.JournalEntry
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import java.text.SimpleDateFormat
import java.util.*

class JournalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalBinding
    private lateinit var dbHelper: JournalDatabaseHelper
    private val availableTags = listOf("Happy", "Work", "Family", "Personal", "Ideas", "Goals")
    private var selectedDate = Calendar.getInstance()
    private var currentMood = 3 // Default neutral mood

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)

        dbHelper = JournalDatabaseHelper(this)
        setupUI()
        loadTodaysEntry()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
        }

        updateDateDisplay()
        setupTagChips()
        setupMoodSelection()

        binding.btnPickDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveJournalEntry()
        }

        binding.moodSlider.addOnChangeListener { _, value, _ ->
            currentMood = value.toInt()
            highlightSelectedMood(currentMood)
        }
    }

    private fun setupMoodSelection() {
        highlightSelectedMood(currentMood)
        binding.moodSlider.value = currentMood.toFloat()
    }

    private fun highlightSelectedMood(mood: Int) {
        val moodViews = listOf(binding.mood1, binding.mood2, binding.mood3, binding.mood4, binding.mood5)
        moodViews.forEachIndexed { index, imageView ->
            val scale = if (index + 1 == mood) 1.2f else 1.0f
            imageView.animate().scaleX(scale).scaleY(scale).duration = 200
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }

    private fun updateDateDisplay() {
        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        binding.tvDay.text = dayFormat.format(selectedDate.time)
        binding.tvDate.text = dateFormat.format(selectedDate.time)
    }

    private fun setupTagChips() {
        binding.chipGroup.removeAllViews()
        availableTags.forEach { tag ->
            val chip = Chip(this).apply {
                text = tag
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(this@JournalActivity, R.color.chip_colors)
                setTextColor(ContextCompat.getColorStateList(this@JournalActivity, R.color.chip_text_colors))
                setEnsureMinTouchTargetSize(false)
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun saveJournalEntry() {
        val entryText = binding.etJournalEntry.text.toString().trim()

        if (entryText.isEmpty()) {
            Toast.makeText(this, "Please write something first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedTags = binding.chipGroup.checkedChipIds
            .joinToString(",") { id ->
                binding.chipGroup.findViewById<Chip>(id)?.text.toString()
            }

        val mappedMood = when (currentMood) {
            1 -> -10 // Angry
            2 -> -5  // Sad
            3 -> 0   // Neutral
            4 -> 5    // Happy
            5 -> 10   // Ecstatic
            else -> 0
        }

        val calendar = selectedDate.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Check if we're editing an existing entry
        val entryId = intent.getLongExtra("ENTRY_ID", -1L)
        val entry = JournalEntry(
            id = if (entryId != -1L) entryId else 0,
            content = entryText,
            mood = mappedMood,
            tags = selectedTags,
            date = calendar.timeInMillis
        )

        if (dbHelper.addOrUpdateEntry(entry)) {
            Toast.makeText(this, "Entry saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to save entry", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTodaysEntry() {
        val calendar = selectedDate.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        val todayStart = calendar.timeInMillis
        val todayEnd = todayStart + 86400000

        // Get all entries for the selected date
        val entries = dbHelper.getEntriesByDate(calendar.timeInMillis)

        // If editing an existing entry, load the specific one
        val entryId = intent.getLongExtra("ENTRY_ID", -1L)
        if (entryId != -1L) {
            entries.find { it.id == entryId }?.let { entry ->
                binding.etJournalEntry.setText(entry.content)
                currentMood = when {
                    entry.mood <= -7 -> 1
                    entry.mood <= -3 -> 2
                    entry.mood <= 3 -> 3
                    entry.mood <= 7 -> 4
                    else -> 5
                }
                binding.moodSlider.value = currentMood.toFloat()
                highlightSelectedMood(currentMood)

                entry.tags.split(",").forEach { tag ->
                    binding.chipGroup.findViewWithTag<Chip>(tag)?.isChecked = true
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.journal_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                supportFinishAfterTransition()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFinishAfterTransition()
    }
}