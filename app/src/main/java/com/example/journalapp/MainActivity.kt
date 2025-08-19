package com.example.journalapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.journalapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    // Word animation variables
    private val rotatingWords = listOf("THOUGHTS", "EXPERIENCES", "EXPECTATIONS", "MEMORIES", "IDEAS")
    private var currentWord = ""
    private var currentWordIndex = 0
    private var isDeletingWord = false
    private var showCursor = true
    private var cursorHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        initUI()
        startCursorAnimation()
    }

    private fun initUI() {
        startWordAnimation()

        // Set PIN input properties
        binding.etPin.filters = arrayOf(InputFilter.LengthFilter(4))
        binding.etPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        binding.btnLogin.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()

            when {
                pin.length != 4 -> showErrorAnimation("Please enter a 4-digit PIN")
                pin != "1234" -> showErrorAnimation("Incorrect PIN")
                else -> animateButtonClick() // Correct PIN
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            showSnackbar("Default PIN is 1234") // For demo purposes only
        }
    }

    private fun startWordAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                if (isDeletingWord) {
                    if (currentWord.isNotEmpty()) {
                        currentWord = currentWord.dropLast(1)
                        updateWordText()
                        handler.postDelayed(this, 80)
                    } else {
                        isDeletingWord = false
                        currentWordIndex = (currentWordIndex + 1) % rotatingWords.size
                        handler.postDelayed(this, 300)
                    }
                } else {
                    val targetWord = rotatingWords[currentWordIndex]
                    if (currentWord.length < targetWord.length) {
                        currentWord = targetWord.take(currentWord.length + 1)
                        updateWordText()
                        handler.postDelayed(this, 80)
                    } else {
                        isDeletingWord = true
                        handler.postDelayed(this, 1500)
                    }
                }
            }
        })
    }

    private fun updateWordText() {
        val cursor = if (showCursor) "|" else ""
        binding.tvWelcome2.text = "SHARE YOUR $currentWord$cursor"
    }

    private fun startCursorAnimation() {
        cursorHandler.post(object : Runnable {
            override fun run() {
                showCursor = !showCursor
                updateWordText()
                cursorHandler.postDelayed(this, 500)
            }
        })
    }

    private fun showErrorAnimation(message: String) {
        try {
            val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
            binding.etPin.startAnimation(shake)
            binding.pinInputLayout.error = message
        } catch (e: Exception) {
            binding.pinInputLayout.error = message
        }
    }

    private fun animateButtonClick() {
        binding.btnLogin.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnLogin.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .withEndAction {
                        navigateToJournalActivity()
                    }
                    .start()
            }
            .start()
    }

    private fun navigateToJournalActivity() {
        val intent = Intent(this, JournalListActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, android.R.color.holo_purple))
            .setTextColor(Color.WHITE)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        cursorHandler.removeCallbacksAndMessages(null)
    }
}