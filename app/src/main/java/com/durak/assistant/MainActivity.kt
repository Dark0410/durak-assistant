package com.durak.assistant

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.durak.assistant.databinding.ActivityMainBinding
import com.durak.assistant.service.OverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            startOverlayService(data)
        } else {
            Toast.makeText(this, "Разрешение на запись экрана отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                requestScreenCapture()
            }
        }

        binding.btnStop.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun requestScreenCapture() {
        try {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(manager.createScreenCaptureIntent())
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка системы захвата: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startOverlayService(data: Intent) {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_RESULT_DATA, data)
        }
        try {
            startForegroundService(intent)
            Toast.makeText(this, "Ассистент запущен! Можете открывать игру.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка запуска: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
