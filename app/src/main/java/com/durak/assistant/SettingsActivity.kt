package com.durak.assistant

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.durak.assistant.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        binding.etApiKey.setText(prefs.getString("gigachat_api_key", ""))

        binding.btnSave.setOnClickListener {
            val key = binding.etApiKey.text.toString()
            prefs.edit().putString("gigachat_api_key", key).apply()
            finish()
        }
    }
}
