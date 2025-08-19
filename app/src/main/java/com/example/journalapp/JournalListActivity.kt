package com.example.journalapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.lottie.LottieAnimationView
import com.example.journalapp.databinding.ActivityJournalListBinding
import com.example.journalapp.db.JournalDatabaseHelper
import com.example.journalapp.models.JournalEntry
import com.example.journalapp.utils.StreakManager
import com.example.journalapp.views.JournalAdapter
import java.util.*

class JournalListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJournalListBinding
    private lateinit var journalAdapter: JournalAdapter
    private lateinit var dbHelper: JournalDatabaseHelper
    private lateinit var streakManager: StreakManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJournalListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimation()
        setupDatabase()
        setupToolbar()
        setupRecyclerView()
        setupFab()
    }

    private fun setupAnimation() {
        try {
            binding.animationView.apply {
                setAnimation(R.raw.watercolor_bg)
                repeatCount = -1
                speed = 0.5f
                alpha = 0.03f
                playAnimation()
            }
        } catch (e: Exception) {
            binding.animationView.visibility = android.view.View.GONE
        }
    }

    private fun setupDatabase() {
        dbHelper = JournalDatabaseHelper(this)
        streakManager = StreakManager(dbHelper)
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Reflections"
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter(
            onClick = { entry ->
                startActivity(Intent(this, JournalActivity::class.java).apply {
                    putExtra("ENTRY_ID", entry.id)
                })
            },
            onDelete = { entry ->
                dbHelper.deleteEntry(entry.id)
                refreshData()
            }
        )

        binding.journalRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@JournalListActivity)
            adapter = journalAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.addEntryFab.setOnClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.journal_list_menu, menu)
        setupSearchView(menu)
        return true
    }

    private fun setupSearchView(menu: Menu) {
        val searchItem = menu.findItem(R.id.action_search)
        (searchItem.actionView as SearchView).apply {
            queryHint = "Search entries..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    filterEntries(query)
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    filterEntries(newText)
                    return true
                }
            })
        }
    }

    private fun filterEntries(query: String) {
        val filtered = if (query.isBlank()) {
            dbHelper.getAllEntries()
        } else {
            // Enhanced search that includes dates
            dbHelper.searchEntries(query)
        }
        journalAdapter.submitList(filtered)
        updateStats(filtered)
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val entries = dbHelper.getAllEntries()
        journalAdapter.submitList(entries)
        updateStats(entries)
    }

    private fun updateStats(entries: List<JournalEntry>) {
        binding.entriesCount.text = entries.size.toString()
        binding.streakCount.text = streakManager.calculateStreak().toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}