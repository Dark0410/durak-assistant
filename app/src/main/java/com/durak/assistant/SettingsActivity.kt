package com.durak.assistant

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.durak.assistant.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("durak_prefs", MODE_PRIVATE)

        // Load existing values
        binding.etApiKey.setText(prefs.getString("gigachat_key", ""))
        
        val deckSize = prefs.getInt("deck_size", 36)
        when (deckSize) {
            24 -> binding.rb24.isChecked = true
            36 -> binding.rb36.isChecked = true
            52 -> binding.rb52.isChecked = true
        }

        // Setup Spinners
        val playersArray = arrayOf("2", "3", "4", "5", "6")
        val playersAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playersArray)
        playersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPlayers.adapter = playersAdapter
        
        val playersCount = prefs.getInt("players_count", 2)
        binding.spinnerPlayers.setSelection(playersArray.indexOf(playersCount.toString()))

        val rulesArray = arrayOf(getString(R.string.rules_simple), getString(R.string.rules_podkidnoy), getString(R.string.rules_perevodnoy))
        val rulesAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, rulesArray)
        rulesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRules.adapter = rulesAdapter
        
        val rulesIndex = prefs.getInt("rules_type", 1) // Default podkidnoy
        binding.spinnerRules.setSelection(rulesIndex)

        binding.btnSave.setOnClickListener {
            val key = binding.etApiKey.text.toString()
            val selectedDeckSize = when {
                binding.rb24.isChecked -> 24
                binding.rb52.isChecked -> 52
                else -> 36
            }
            val selectedPlayers = binding.spinnerPlayers.selectedItem.toString().toInt()
            val selectedRules = binding.spinnerRules.selectedItemId.toInt()

            prefs.edit().apply {
                putString("gigachat_key", key)
                putInt("deck_size", selectedDeckSize)
                putInt("players_count", selectedPlayers)
                putInt("rules_type", selectedRules)
                apply()
            }
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
