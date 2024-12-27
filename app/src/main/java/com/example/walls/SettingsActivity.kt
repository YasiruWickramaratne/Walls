package com.example.walls

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class SettingsActivity : AppCompatActivity() {

    private lateinit var etApiKey: EditText
    private lateinit var btnSaveApiKey: Button
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etApiKey = findViewById(R.id.etApiKey)
        btnSaveApiKey = findViewById(R.id.btnSaveApiKey)
        sharedPref = getSharedPreferences("WallsPrefs", Context.MODE_PRIVATE)

        // Load the saved API key if it exists
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

        // Initially disable the save button
        btnSaveApiKey.isEnabled = false
    }

    private fun saveApiKey(apiKey: String) {
        with(sharedPref.edit()) {
            putString("API_KEY", apiKey)
            apply()
        }
    }
}