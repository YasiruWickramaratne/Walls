package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.addTextChangedListener
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    private lateinit var etApiKey: EditText
    private lateinit var btnSaveApiKey: Button
    private lateinit var sharedPref: SharedPreferences
    private lateinit var switchAutoChange: SwitchCompat
    private lateinit var spinnerInterval: Spinner
    private lateinit var spinnerScreen: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etApiKey = findViewById(R.id.etApiKey)
        btnSaveApiKey = findViewById(R.id.btnSaveApiKey)
        switchAutoChange = findViewById(R.id.switchAutoChange)
        spinnerInterval = findViewById(R.id.spinnerInterval)
        spinnerScreen = findViewById(R.id.spinnerScreen)
        sharedPref = getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)

        setupApiKeySection()
        setupAutoWallpaperSection()
    }

    private fun setupApiKeySection() {
        val savedApiKey = sharedPref.getString("API_KEY", "") ?: ""
        etApiKey.setText(savedApiKey)

        etApiKey.addTextChangedListener { text ->
            val newApiKey = text.toString().trim()
            btnSaveApiKey.isEnabled = newApiKey != savedApiKey && newApiKey.isNotEmpty()
        }

        btnSaveApiKey.setOnClickListener {
            val newApiKey = etApiKey.text.toString().trim()
            if (newApiKey.isNotEmpty()) {
                saveApiKey(newApiKey)
                Toast.makeText(this, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid API Key", Toast.LENGTH_SHORT).show()
            }
        }
        btnSaveApiKey.isEnabled = false
    }

    private fun setupAutoWallpaperSection() {
        val autoChangeEnabled = sharedPref.getBoolean("AUTO_CHANGE_ENABLED", false)
        val selectedInterval = sharedPref.getLong("AUTO_CHANGE_INTERVAL", 15 * 60 * 1000L) // Default 15 min
        val selectedScreen = sharedPref.getInt("WALLPAPER_SCREEN", 0) // Default Home Screen

        switchAutoChange.isChecked = autoChangeEnabled

        val intervalOptions = resources.getStringArray(R.array.auto_change_intervals)
        val intervalValues = resources.getIntArray(R.array.auto_change_interval_millis)
        val intervalAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intervalOptions)
        spinnerInterval.adapter = intervalAdapter
        spinnerInterval.setSelection(intervalValues.indexOf(selectedInterval.toInt()))

        val screenOptions = resources.getStringArray(R.array.wallpaper_screens)
        val screenAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, screenOptions)
        spinnerScreen.adapter = screenAdapter
        spinnerScreen.setSelection(selectedScreen)

        switchAutoChange.setOnCheckedChangeListener { _, isChecked ->
            val intervalMillis = intervalValues[spinnerInterval.selectedItemPosition].toLong()
            val screenValue = spinnerScreen.selectedItemPosition
            sharedPref.edit().putBoolean("AUTO_CHANGE_ENABLED", isChecked).putLong("AUTO_CHANGE_INTERVAL", intervalMillis).putInt("WALLPAPER_SCREEN", screenValue).apply()
            if (isChecked) {
                scheduleAutoWallpaperChange(intervalMillis)
            } else {
                cancelAutoWallpaperChange()
            }
        }

        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (switchAutoChange.isChecked) {
                    val intervalMillis = intervalValues[position].toLong()
                    val screenValue = spinnerScreen.selectedItemPosition
                    sharedPref.edit().putLong("AUTO_CHANGE_INTERVAL", intervalMillis).putInt("WALLPAPER_SCREEN", screenValue).apply()
                    scheduleAutoWallpaperChange(intervalMillis)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerScreen.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (switchAutoChange.isChecked) {
                    val intervalMillis = intervalValues[spinnerInterval.selectedItemPosition].toLong()
                    val screenValue = position
                    sharedPref.edit().putInt("WALLPAPER_SCREEN", screenValue).putLong("AUTO_CHANGE_INTERVAL", intervalMillis).apply()
                    scheduleAutoWallpaperChange(intervalMillis)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initial scheduling if enabled on app start
        if (autoChangeEnabled) {
            scheduleAutoWallpaperChange(selectedInterval)
        }
    }

    private fun saveApiKey(apiKey: String) {
        with(sharedPref.edit()) {
            putString("API_KEY", apiKey)
            apply()
        }
    }

    private fun scheduleAutoWallpaperChange(intervalMillis: Long) {
        // Convert milliseconds to minutes
        val intervalMinutes = intervalMillis / (60 * 1000)
        
        // Validate interval (minimum 5 minutes)
        if (intervalMinutes < 5) {
            Log.e("SettingsActivity", "Invalid interval: $intervalMinutes minutes (minimum is 5 minutes)")
            return
        }

        Log.d("SettingsActivity", "Scheduling auto wallpaper change with interval: $intervalMinutes minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
            intervalMinutes,
            TimeUnit.MINUTES,
            5, // flex interval
            TimeUnit.MINUTES
        )
        .setConstraints(constraints)
        .setBackoffCriteria(
            BackoffPolicy.LINEAR,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS
        )
        .build()

        // Use REPLACE policy to ensure only one worker is scheduled
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "auto_wallpaper_change",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )

        Log.d("SettingsActivity", "Auto wallpaper change scheduled for every $intervalMinutes minutes")
    }

    private fun cancelAutoWallpaperChange() {
        WorkManager.getInstance(this).cancelUniqueWork("auto_wallpaper_change")
        Log.d("SettingsActivity", "Auto wallpaper change cancelled")
    }
}